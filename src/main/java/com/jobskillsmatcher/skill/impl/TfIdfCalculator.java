package com.jobskillsmatcher.skill.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class TfIdfCalculator {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "with",
            "as", "at", "by", "is", "are", "was", "were", "be", "been", "being",
            "it", "its", "this", "that", "these", "those", "such", "from", "into",
            "than", "then", "over", "so", "not", "no", "can", "may", "might", "will",
            "shall", "should", "would", "do", "does", "did", "has", "have", "had"
    );

    private TfIdfCalculator() {
    }

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] raw = TOKEN_SPLIT.split(text.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>(raw.length);
        for (String tok : raw) {
            if (tok.isEmpty() || STOP_WORDS.contains(tok) || tok.length() < 2) {
                continue;
            }
            out.add(tok);
        }
        return out;
    }

    public static Result compute(List<String> documentIds, List<String> documents) {
        if (documentIds.size() != documents.size()) {
            throw new IllegalArgumentException("documentIds and documents must be the same size");
        }
        int n = documents.size();
        List<List<String>> tokenizedDocs = new ArrayList<>(n);
        Set<String> vocabSet = new TreeSet<>();
        for (String doc : documents) {
            List<String> tokens = tokenize(doc);
            tokenizedDocs.add(tokens);
            vocabSet.addAll(tokens);
        }
        List<String> vocabulary = new ArrayList<>(vocabSet);

        Map<String, Integer> vocabIndex = new HashMap<>(vocabulary.size() * 2);
        for (int i = 0; i < vocabulary.size(); i++) {
            vocabIndex.put(vocabulary.get(i), i);
        }

        int[] df = new int[vocabulary.size()];
        for (List<String> tokens : tokenizedDocs) {
            Set<String> seen = new HashSet<>(tokens);
            for (String tok : seen) {
                df[vocabIndex.get(tok)]++;
            }
        }

        double[] idf = new double[vocabulary.size()];
        for (int i = 0; i < vocabulary.size(); i++) {
            idf[i] = Math.log((double) n / (1 + df[i])) + 1.0;
        }

        Map<String, double[]> vectors = new HashMap<>(n * 2);
        for (int d = 0; d < n; d++) {
            List<String> tokens = tokenizedDocs.get(d);
            double[] vec = new double[vocabulary.size()];
            if (tokens.isEmpty()) {
                vectors.put(documentIds.get(d), vec);
                continue;
            }
            int total = tokens.size();
            int[] tf = new int[vocabulary.size()];
            for (String tok : tokens) {
                tf[vocabIndex.get(tok)]++;
            }
            double norm = 0.0;
            for (int i = 0; i < vec.length; i++) {
                double weight = (tf[i] / (double) total) * idf[i];
                vec[i] = weight;
                norm += weight * weight;
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < vec.length; i++) {
                    vec[i] /= norm;
                }
            }
            vectors.put(documentIds.get(d), vec);
        }

        return new Result(Collections.unmodifiableList(vocabulary), vectors);
    }

    public record Result(List<String> vocabulary, Map<String, double[]> vectors) {
        public double[] vectorFor(String documentId) {
            double[] v = vectors.get(documentId);
            return v == null ? null : Arrays.copyOf(v, v.length);
        }
    }
}
