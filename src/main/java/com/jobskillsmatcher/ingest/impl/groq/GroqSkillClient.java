package com.jobskillsmatcher.ingest.impl.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class GroqSkillClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500L;
    private static final int MAX_TEXT_CHARS = 12_000;
    private static final String CHAT_PATH = "/openai/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
            You extract ESCO-style professional skills from job postings and CVs.
            You are given some text and a list of skills that were ALREADY detected.
            Return ONLY skills that are present in the text but NOT in the already-detected list.
            Respond with STRICT JSON and nothing else, in exactly this shape:
            {"skills":[{"skill":"<short skill name>","requirement":"REQUIRED|PREFERRED"}]}
            Use REQUIRED for must-have skills and PREFERRED for nice-to-have skills.
            Use concise, well-known skill names (e.g. "React", "PostgreSQL", "Kubernetes").
            If there is nothing new, return {"skills":[]}. No prose, no markdown.""";

    private final RestClient groqRestClient;
    private final Bucket groqRateLimiter;
    private final GroqProperties props;
    private final ObjectMapper objectMapper;

    public Optional<List<GroqSkill>> suggestMissingSkills(String text, List<String> alreadyDetectedLabels) {
        if (!props.usable() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (!groqRateLimiter.tryConsume(1)) {
            log.debug("Groq local rate limit exhausted; skipping enrichment");
            return Optional.empty();
        }

        String trimmed = text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
        String detected = alreadyDetectedLabels == null || alreadyDetectedLabels.isEmpty()
                ? "(none)"
                : String.join(", ", alreadyDetectedLabels);
        String userPrompt = "Already detected: " + detected + "\n\nText:\n" + trimmed;

        Map<String, Object> body = Map.of(
                "model", props.model(),
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)));

        HttpServerErrorException lastServerError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String raw = groqRestClient.post()
                        .uri(CHAT_PATH)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return Optional.of(parse(raw));
            } catch (HttpClientErrorException.TooManyRequests ex) {
                sleepBackoff(attempt);
            } catch (HttpServerErrorException ex) {
                lastServerError = ex;
                sleepBackoff(attempt);
            } catch (Exception ex) {
                // Bad request, parse failure, connection reset, etc. — give up gracefully.
                log.warn("Groq enrichment failed, falling back to algorithm result: {}", ex.getMessage());
                return Optional.empty();
            }
        }
        if (lastServerError != null) {
            log.warn("Groq upstream failed after {} attempts: {}",
                    MAX_ATTEMPTS, lastServerError.getMessage());
        }
        return Optional.empty();
    }

    private List<GroqSkill> parse(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || !content.isTextual()) {
            return List.of();
        }
        String json = stripFences(content.asText());
        JsonNode skills = objectMapper.readTree(json).path("skills");
        if (!skills.isArray()) {
            return List.of();
        }
        List<GroqSkill> out = new ArrayList<>();
        for (JsonNode node : skills) {
            String skill = node.path("skill").asText("").trim();
            if (skill.isEmpty()) {
                continue;
            }
            String requirement = node.path("requirement").asText("PREFERRED").trim().toUpperCase();
            out.add(new GroqSkill(skill, requirement));
        }
        return out;
    }

    private static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) {
                t = t.substring(firstNl + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private static void sleepBackoff(int attempt) {
        long sleep = BASE_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public record GroqSkill(String skill, String requirement) { }
}
