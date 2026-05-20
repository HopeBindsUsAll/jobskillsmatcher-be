package com.jobskillsmatcher.ingest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.job.model.Requirement;
import com.jobskillsmatcher.skill.impl.jpa.Skill;
import com.jobskillsmatcher.skill.SkillRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Golden tests for {@link JobSkillExtractor}. Each fixture under
 * {@code src/test/resources/fixtures/jobs/} pairs a {@code .txt} description
 * with a {@code .expected.json} listing the expected REQUIRED and PREFERRED
 * skill IDs. Adding a new fixture pair grows the test surface for free.
 */
class JobSkillExtractorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JobSkillExtractor extractor;

    @BeforeAll
    static void setUp() {
        SkillRepository skillRepository = mock(SkillRepository.class);
        when(skillRepository.findAll()).thenReturn(seedSkills());
        extractor = new JobSkillExtractor(skillRepository);
        extractor.rebuildIndex();
    }

    @TestFactory
    Stream<DynamicTest> goldenFixtures() throws IOException {
        Path root = Paths.get("src/test/resources/fixtures/jobs");
        return Files.list(root)
                .filter(p -> p.toString().endsWith(".txt"))
                .sorted()
                .map(JobSkillExtractorTest::buildTest);
    }

    private static DynamicTest buildTest(Path descriptionPath) {
        String fileName = descriptionPath.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - ".txt".length());
        Path expectedPath = descriptionPath.resolveSibling(stem + ".expected.json");
        return DynamicTest.dynamicTest(stem, () -> {
            String description = Files.readString(descriptionPath);
            Expected expected = MAPPER.readValue(expectedPath.toFile(), Expected.class);

            Map<String, Requirement> actual = extractor.extract(description);

            Set<String> actualRequired = actual.entrySet().stream()
                    .filter(e -> e.getValue() == Requirement.REQUIRED)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(TreeSet::new));
            Set<String> actualPreferred = actual.entrySet().stream()
                    .filter(e -> e.getValue() == Requirement.PREFERRED)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertThat(actualRequired)
                    .as("required skills for %s", stem)
                    .containsExactlyInAnyOrderElementsOf(expected.required());
            assertThat(actualPreferred)
                    .as("preferred skills for %s", stem)
                    .containsExactlyInAnyOrderElementsOf(expected.preferred());
        });
    }

    private static List<Skill> seedSkills() {
        List<Skill> out = new ArrayList<>();
        // (id, preferredLabel, [altLabels])
        out.add(skill("esco/skill/java", "Java", "jdk", "jvm", "java se"));
        out.add(skill("esco/skill/python", "Python", "python3", "cpython"));
        out.add(skill("esco/skill/javascript", "JavaScript", "js", "ecmascript"));
        out.add(skill("esco/skill/typescript", "TypeScript", "ts"));
        out.add(skill("esco/skill/sql", "SQL", "structured query language"));
        out.add(skill("esco/skill/kotlin", "Kotlin"));
        out.add(skill("esco/skill/swift", "Swift"));
        out.add(skill("esco/skill/object-oriented-programming", "Object-Oriented Programming", "oop"));
        out.add(skill("esco/skill/git", "Git", "version control", "vcs"));
        out.add(skill("esco/skill/html", "HTML", "html5"));
        out.add(skill("esco/skill/css", "CSS", "css3"));
        out.add(skill("esco/skill/react", "React", "react.js", "reactjs"));
        out.add(skill("esco/skill/nodejs", "Node.js", "node"));
        out.add(skill("esco/skill/rest-api", "REST API Design", "rest", "restful services"));
        out.add(skill("esco/skill/graphql", "GraphQL"));
        out.add(skill("esco/skill/spring-boot", "Spring Boot", "spring"));
        out.add(skill("esco/skill/postgresql", "PostgreSQL", "postgres"));
        out.add(skill("esco/skill/redis", "Redis"));
        out.add(skill("esco/skill/machine-learning", "Machine Learning", "ml"));
        out.add(skill("esco/skill/deep-learning", "Deep Learning", "neural networks"));
        out.add(skill("esco/skill/pandas", "pandas"));
        out.add(skill("esco/skill/numpy", "NumPy"));
        out.add(skill("esco/skill/tensorflow", "TensorFlow", "keras"));
        out.add(skill("esco/skill/data-analysis", "Data Analysis", "exploratory data analysis", "eda"));
        out.add(skill("esco/skill/linux", "Linux", "unix"));
        out.add(skill("esco/skill/shell-scripting", "Shell Scripting", "bash", "zsh"));
        out.add(skill("esco/skill/http-protocol", "HTTP Protocol"));
        out.add(skill("esco/skill/cryptography", "Applied Cryptography", "crypto"));
        out.add(skill("esco/skill/web-security", "Web Application Security", "owasp", "appsec"));
        out.add(skill("esco/skill/authentication", "Authentication and Authorization", "authn", "authz", "oauth2", "jwt"));
        out.add(skill("esco/skill/aws", "Amazon Web Services", "aws", "amazon cloud"));
        out.add(skill("esco/skill/docker", "Docker", "containers"));
        out.add(skill("esco/skill/kubernetes", "Kubernetes", "k8s"));
        out.add(skill("esco/skill/ci-cd", "Continuous Integration and Delivery", "ci", "cd", "ci/cd"));
        out.add(skill("esco/skill/terraform", "Terraform", "infrastructure as code", "iac"));
        out.add(skill("esco/skill/android-development", "Android Application Development", "android"));
        out.add(skill("esco/skill/ios-development", "iOS Application Development", "ios"));
        out.add(skill("esco/skill/react-native", "React Native"));
        out.add(skill("esco/skill/figma", "Figma"));
        out.add(skill("esco/skill/user-research", "User Research", "ux research"));
        out.add(skill("esco/skill/accessibility", "Accessibility (a11y)", "a11y", "wcag"));
        out.add(skill("esco/skill/unit-testing", "Unit Testing", "junit", "pytest", "jest"));
        out.add(skill("esco/skill/integration-testing", "Integration Testing"));
        out.add(skill("esco/skill/test-driven-development", "Test-Driven Development", "tdd"));
        out.add(skill("esco/skill/selenium", "Selenium", "webdriver"));
        out.add(skill("esco/skill/distributed-systems", "Distributed Systems"));
        out.add(skill("esco/skill/microservices", "Microservices Architecture"));
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

    @Test
    void preferredHeaderBucketsFollowingSkillsAsPreferred() {
        String desc = """
                Required: Java, SQL.
                Nice to have: Kotlin, GraphQL.
                """;
        Map<String, Requirement> actual = extractor.extract(desc);

        assertThat(actual)
                .containsEntry("esco/skill/java", Requirement.REQUIRED)
                .containsEntry("esco/skill/sql", Requirement.REQUIRED)
                .containsEntry("esco/skill/kotlin", Requirement.PREFERRED)
                .containsEntry("esco/skill/graphql", Requirement.PREFERRED);
    }

    @Test
    void preferredColonHeaderRecognised() {
        String desc = """
                You will work with Java and PostgreSQL daily.
                Preferred: TypeScript, Docker.
                """;
        Map<String, Requirement> actual = extractor.extract(desc);

        assertThat(actual)
                .containsEntry("esco/skill/java", Requirement.REQUIRED)
                .containsEntry("esco/skill/postgresql", Requirement.REQUIRED)
                .containsEntry("esco/skill/typescript", Requirement.PREFERRED)
                .containsEntry("esco/skill/docker", Requirement.PREFERRED);
    }

    @Test
    void noPreferredSectionMeansNoPreferredEntries() {
        String desc = """
                Requirements: Python and SQL with strong unit testing.
                Responsibilities: ship features end-to-end.
                """;
        Map<String, Requirement> actual = extractor.extract(desc);

        assertThat(actual.values())
                .as("descriptions without a preferred header must produce zero PREFERRED entries")
                .doesNotContain(Requirement.PREFERRED);
        assertThat(actual)
                .containsEntry("esco/skill/python", Requirement.REQUIRED)
                .containsEntry("esco/skill/sql", Requirement.REQUIRED);
    }

    @Test
    void skillSeenInBothSectionsResolvesAsRequired() {
        String desc = """
                Required: Java, Docker.
                Nice to have: Docker, Kubernetes.
                """;
        Map<String, Requirement> actual = extractor.extract(desc);

        assertThat(actual)
                .containsEntry("esco/skill/docker", Requirement.REQUIRED)
                .containsEntry("esco/skill/kubernetes", Requirement.PREFERRED);
    }

    private record Expected(Set<String> required, Set<String> preferred) {
        Expected {
            required = required == null ? Set.of() : required;
            preferred = preferred == null ? Set.of() : preferred;
        }
    }

    @SuppressWarnings("unused")
    private static InputStream stream(Class<?> clazz, String resource) {
        return clazz.getClassLoader().getResourceAsStream(resource);
    }
}
