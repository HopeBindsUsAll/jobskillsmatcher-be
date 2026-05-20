package com.jobskillsmatcher.skill.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TfIdfCalculatorTest {

    @Test
    void successComputeVocabularyAndNormalisedVectors() {
        List<String> ids = List.of("a", "b", "c");
        List<String> docs = List.of(
                "java virtual machine",
                "python programming language",
                "java language"
        );

        TfIdfCalculator.Result result = TfIdfCalculator.compute(ids, docs);

        assertThat(result.vocabulary()).containsExactly(
                "java", "language", "machine", "programming", "python", "virtual"
        );

        for (String id : ids) {
            double[] vector = result.vectorFor(id);
            double norm = 0.0;
            for (double v : vector) {
                norm += v * v;
            }
            assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
            assertThat(vector).hasSize(result.vocabulary().size());
        }
    }

    @Test
    void successTokenizeStripsStopWordsAndPunctuation()  {
        List<String> tokens = TfIdfCalculator.tokenize("The Java Virtual Machine is fast, and reliable!");

        assertThat(tokens).containsExactly("java", "virtual", "machine", "fast", "reliable");
    }

    @Test
    void successTokenizeReturnsEmptyForBlank() {
        assertThat(TfIdfCalculator.tokenize(null)).isEmpty();
        assertThat(TfIdfCalculator.tokenize("   ")).isEmpty();
    }

    @Test
    void failedComputeWithMismatchedSizes() {
        assertThatThrownBy(() -> TfIdfCalculator.compute(
                List.of("a", "b"),
                List.of("only one doc")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
