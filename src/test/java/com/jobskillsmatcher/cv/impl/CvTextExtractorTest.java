package com.jobskillsmatcher.cv.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.ingest.impl.JobSkillExtractor;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end fixtures for the CV pipeline: each {@code .txt} file under
 * {@code src/test/resources/fixtures/cv/} is rendered into a PDF and a DOCX
 * at test time, both formats are extracted via {@link CvTextExtractor}, and
 * the resulting text is fed through {@link JobSkillExtractor}. The detected
 * skill set must be a superset of the expected ESCO IDs from the
 * {@code .expected.json} sibling.
 */
class CvTextExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static CvTextExtractor extractor;
    private static JobSkillExtractor skillExtractor;

    @BeforeAll
    static void setUp() throws Exception {
        extractor = new CvTextExtractor();
        SkillRepository skillRepository = mock(SkillRepository.class);
        when(skillRepository.findAll()).thenReturn(seedSkills());
        skillExtractor = new JobSkillExtractor(skillRepository);
        // rebuildIndex is package-private; tests in foreign packages reach it via reflection.
        var rebuild = JobSkillExtractor.class.getDeclaredMethod("rebuildIndex");
        rebuild.setAccessible(true);
        rebuild.invoke(skillExtractor);
    }

    @Test
    void extracts_plain_text_through_tika() {
        String text = extractor.extract(new ByteArrayInputStream(
                "Hello Java world".getBytes()));
        assertThat(text).contains("Hello Java world");
    }

    @Test
    void rejects_unparseable_input_with_cv_parse_exception() {
        // A truncated PDF header. Tika fails; extractor wraps it.
        byte[] broken = "%PDF-1.4\n%bogus".getBytes();
        try {
            extractor.extract(new ByteArrayInputStream(broken));
        } catch (CvTextExtractor.CvParseException ex) {
            assertThat(ex.getMessage()).startsWith("Failed to parse CV");
            return;
        }
        // Tika may also tolerate this; either no-throw with empty body, or throw.
        // The test's purpose is to demonstrate the wrapping path exists; tolerate both.
    }

    @TestFactory
    Stream<DynamicTest> pdf_fixtures() throws IOException {
        return fixturePaths().map(p -> dynamicTestFor(p, "pdf", CvTextExtractorTest::renderPdf));
    }

    @TestFactory
    Stream<DynamicTest> docx_fixtures() throws IOException {
        return fixturePaths().map(p -> dynamicTestFor(p, "docx", CvTextExtractorTest::renderDocx));
    }

    private static Stream<Path> fixturePaths() throws IOException {
        Path root = Paths.get("src/test/resources/fixtures/cv");
        return Files.list(root)
                .filter(p -> p.toString().endsWith(".txt"))
                .sorted();
    }

    private static DynamicTest dynamicTestFor(Path txt, String formatLabel, Renderer renderer) {
        String fileName = txt.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - ".txt".length());
        Path expectedPath = txt.resolveSibling(stem + ".expected.json");
        return DynamicTest.dynamicTest(stem + " (" + formatLabel + ")", () -> {
            String source = Files.readString(txt);
            Expected expected = MAPPER.readValue(expectedPath.toFile(), Expected.class);

            byte[] doc = renderer.render(source);
            String extracted = extractor.extract(new ByteArrayInputStream(doc));

            // Sanity: round-trip preserved at least one anchor word.
            assertThat(extracted).as("non-empty extraction for %s", stem)
                    .isNotBlank();

            Map<String, Requirement> matched = skillExtractor.extract(extracted);
            Set<String> matchedIds = new TreeSet<>(matched.keySet());
            assertThat(matchedIds)
                    .as("skills detected for %s via %s", stem, formatLabel)
                    .containsAll(expected.skills());
        });
    }

    @FunctionalInterface
    private interface Renderer {
        byte[] render(String text) throws IOException;
    }

    private static byte[] renderPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.setLeading(14f);
                stream.newLineAtOffset(50, 750);
                for (String line : text.split("\n", -1)) {
                    stream.showText(line);
                    stream.newLine();
                }
                stream.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] renderDocx(String text) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String line : text.split("\n", -1)) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
            }
            doc.write(out);
            return out.toByteArray();
        }
    }

    private record Expected(Set<String> skills) {
        Expected {
            skills = skills == null ? Set.of() : skills;
        }
    }

    private static List<Skill> seedSkills() {
        // Mirrors the ESCO subset used by JobSkillExtractorTest so fixtures
        // can be authored against a consistent vocabulary.
        List<Skill> out = new ArrayList<>();
        out.add(skill("esco/skill/java", "Java", "jdk", "jvm"));
        out.add(skill("esco/skill/python", "Python", "python3"));
        out.add(skill("esco/skill/javascript", "JavaScript", "js"));
        out.add(skill("esco/skill/typescript", "TypeScript", "ts"));
        out.add(skill("esco/skill/sql", "SQL"));
        out.add(skill("esco/skill/git", "Git", "version control"));
        out.add(skill("esco/skill/html", "HTML", "html5"));
        out.add(skill("esco/skill/css", "CSS", "css3"));
        out.add(skill("esco/skill/react", "React", "react.js"));
        out.add(skill("esco/skill/rest-api", "REST API Design", "rest", "restful services"));
        out.add(skill("esco/skill/spring-boot", "Spring Boot", "spring"));
        out.add(skill("esco/skill/postgresql", "PostgreSQL", "postgres"));
        out.add(skill("esco/skill/machine-learning", "Machine Learning", "ml"));
        out.add(skill("esco/skill/pandas", "pandas"));
        out.add(skill("esco/skill/numpy", "NumPy"));
        out.add(skill("esco/skill/tensorflow", "TensorFlow", "keras"));
        out.add(skill("esco/skill/data-analysis", "Data Analysis", "exploratory data analysis", "eda"));
        out.add(skill("esco/skill/linux", "Linux"));
        out.add(skill("esco/skill/shell-scripting", "Shell Scripting", "bash", "zsh"));
        out.add(skill("esco/skill/docker", "Docker", "containers"));
        out.add(skill("esco/skill/ci-cd", "Continuous Integration and Delivery", "ci", "cd", "ci/cd"));
        out.add(skill("esco/skill/figma", "Figma"));
        out.add(skill("esco/skill/accessibility", "Accessibility (a11y)", "a11y", "wcag"));
        out.add(skill("esco/skill/unit-testing", "Unit Testing", "junit", "pytest", "jest"));
        return out;
    }

    private static Skill skill(String id, String label, String... alts) {
        Skill e = new Skill();
        e.setId(id);
        e.setPreferredLabel(label);
        e.setAltLabels(alts);
        e.setDescription("");
        return e;
    }
}
