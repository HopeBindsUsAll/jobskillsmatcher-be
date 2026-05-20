package com.jobskillsmatcher.matching.impl;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedJaccardCalculatorTest {

    private static final Offset<Double> EPS = Offset.offset(1e-9);

    @Test
    void successIdenticalWeightsReturnsOne() {
        Map<String, Double> a = Map.of("java", 1.0, "sql", 1.0);
        Map<String, Double> b = Map.of("java", 1.0, "sql", 1.0);

        double j = WeightedJaccardCalculator.compute(a, b, 0.0);

        assertThat(j).isCloseTo(1.0, EPS);
    }

    @Test
    void successDisjointSetsReturnZero() {
        Map<String, Double> a = Map.of("java", 1.0);
        Map<String, Double> b = Map.of("python", 1.0);

        double j = WeightedJaccardCalculator.compute(a, b, 0.0);

        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void successHandComputedPartialOverlap() {
        // student: java=1.0, sql=0.66
        // job:     java=1.0, docker=0.5
        // union: {java, sql, docker}
        //   java:   min=1.0,  max=1.0
        //   sql:    min=0.0,  max=0.66
        //   docker: min=0.0,  max=0.5
        // num=1.0, den=2.16, J = 1.0/2.16 ≈ 0.46296...
        Map<String, Double> student = Map.of("java", 1.0, "sql", 0.66);
        Map<String, Double> job = Map.of("java", 1.0, "docker", 0.5);

        double j = WeightedJaccardCalculator.compute(student, job, 0.0);

        assertThat(j).isCloseTo(1.0 / 2.16, Offset.offset(1e-6));
    }

    @Test
    void successBothEmptyReturnsZero() {
        double j = WeightedJaccardCalculator.compute(Map.of(), Map.of(), 0.0);
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void successOneEmptyReturnsZero() {
        double j = WeightedJaccardCalculator.compute(Map.of("java", 1.0), Map.of(), 0.0);
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void successNullArgsTreatedAsEmpty() {
        double j = WeightedJaccardCalculator.compute(null, Map.of("java", 1.0), 0.0);
        // The student-side is empty so num=0 / den=1 = 0.
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void successZeroDenominatorReturnsZero() {
        // Both entries 0.0 → max=0 across union → den=0.
        Map<String, Double> a = Map.of("java", 0.0);
        Map<String, Double> b = Map.of("java", 0.0);

        double j = WeightedJaccardCalculator.compute(a, b, 0.0);

        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void successPenaltyReducesScoreWhenStudentHasExtras() {
        // student has java + 2 extras; job only wants java.
        Map<String, Double> student = Map.of("java", 1.0, "rust", 1.0, "go", 1.0);
        Map<String, Double> job = Map.of("java", 1.0);

        double base = WeightedJaccardCalculator.compute(student, job, 0.0);
        double penalised = WeightedJaccardCalculator.compute(student, job, 0.1);

        assertThat(penalised).isLessThan(base);
        // 2 extras × 0.1 = 0.2 deduction from base.
        assertThat(penalised).isCloseTo(Math.max(0.0, base - 0.2), Offset.offset(1e-6));
    }

    @Test
    void successPenaltyClampsAtZero() {
        Map<String, Double> student = Map.of("java", 1.0, "rust", 1.0);
        Map<String, Double> job = Map.of("python", 1.0);

        // Disjoint → base = 0; penalty must not produce a negative result.
        double j = WeightedJaccardCalculator.compute(student, job, 0.5);

        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void coverageIgnoresUnrelatedStudentSkills() {
        // student has java + 5 unrelated skills; job only wants java.
        Map<String, Double> student = Map.of(
                "java", 1.0,
                "rust", 1.0, "go", 1.0, "elixir", 1.0, "haskell", 1.0, "lisp", 1.0);
        Map<String, Double> job = Map.of("java", 1.0);

        double coverage = WeightedJaccardCalculator.computeCoverage(student, job);
        double symmetric = WeightedJaccardCalculator.compute(student, job, 0.0);

        // Coverage is 1.0 — student fully covers what the job asks for.
        assertThat(coverage).isCloseTo(1.0, EPS);
        // The old symmetric Jaccard punishes the same student heavily for those extras.
        assertThat(symmetric).isLessThan(0.2);
    }

    @Test
    void coveragePartialMatchScoresByJobWeightOnly() {
        // job: java=1.0 (required), docker=0.5 (preferred). student has only java=1.0.
        // Expected: num=min(1,1)+min(0,0.5)=1.0, den=1.0+0.5=1.5 → 0.6666...
        Map<String, Double> student = Map.of("java", 1.0);
        Map<String, Double> job = Map.of("java", 1.0, "docker", 0.5);

        double j = WeightedJaccardCalculator.computeCoverage(student, job);

        assertThat(j).isCloseTo(1.0 / 1.5, Offset.offset(1e-6));
    }

    @Test
    void coverageReturnsZeroWhenJobEmpty() {
        double j = WeightedJaccardCalculator.computeCoverage(Map.of("java", 1.0), Map.of());
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void coverageReturnsZeroWhenStudentMissingEverything() {
        double j = WeightedJaccardCalculator.computeCoverage(Map.of(), Map.of("java", 1.0));
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void coverageHandlesNullStudentAsEmpty() {
        double j = WeightedJaccardCalculator.computeCoverage(null, Map.of("java", 1.0));
        assertThat(j).isCloseTo(0.0, EPS);
    }

    @Test
    void coverageCapsCreditAtJobWeight() {
        // student is over-leveled (1.0) on a preferred-weight (0.5) skill — credit caps at 0.5,
        // not 1.0, so 1.0/0.5 still equals 1.0 (full coverage of the preferred slot).
        Map<String, Double> student = Map.of("docker", 1.0);
        Map<String, Double> job = Map.of("docker", 0.5);

        double j = WeightedJaccardCalculator.computeCoverage(student, job);

        assertThat(j).isCloseTo(1.0, EPS);
    }
}
