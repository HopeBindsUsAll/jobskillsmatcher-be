package com.jobskillsmatcher.matching.impl;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CosineCalculatorTest {

    private static final Offset<Double> EPS = Offset.offset(1e-9);

    @Test
    void successSelfReturnsOne() {
        double[] v = {1.0, 2.0, 3.0};
        assertThat(CosineCalculator.cosine(v, v)).isCloseTo(1.0, EPS);
    }

    @Test
    void successOrthogonalVectorsReturnZero() {
        double[] a = {1.0, 0.0};
        double[] b = {0.0, 1.0};
        assertThat(CosineCalculator.cosine(a, b)).isCloseTo(0.0, EPS);
    }

    @Test
    void successSymmetric() {
        double[] a = {0.5, 0.5, 0.5, 0.5};
        double[] b = {1.0, 0.0, 1.0, 0.0};
        double ab = CosineCalculator.cosine(a, b);
        double ba = CosineCalculator.cosine(b, a);
        assertThat(ab).isCloseTo(ba, EPS);
    }

    @Test
    void successHandComputedAcuteAngle() {
        // a=(1,1), b=(1,0). dot=1, |a|=√2, |b|=1 → 1/√2 ≈ 0.7071068
        double[] a = {1.0, 1.0};
        double[] b = {1.0, 0.0};
        assertThat(CosineCalculator.cosine(a, b))
                .isCloseTo(1.0 / Math.sqrt(2.0), Offset.offset(1e-9));
    }

    @Test
    void successZeroVectorReturnsZero() {
        double[] a = {0.0, 0.0, 0.0};
        double[] b = {1.0, 2.0, 3.0};
        assertThat(CosineCalculator.cosine(a, b)).isCloseTo(0.0, EPS);
    }

    @Test
    void successNullOrEmptyReturnsZero() {
        assertThat(CosineCalculator.cosine(null, new double[]{1.0})).isCloseTo(0.0, EPS);
        assertThat(CosineCalculator.cosine(new double[]{1.0}, null)).isCloseTo(0.0, EPS);
        assertThat(CosineCalculator.cosine(new double[0], new double[0])).isCloseTo(0.0, EPS);
    }

    @Test
    void failedLengthMismatchThrows() {
        assertThatThrownBy(() -> CosineCalculator.cosine(new double[]{1.0, 2.0}, new double[]{1.0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same length");
    }

    @Test
    void successResultClampedToValidRange() {
        // Construct vectors where floating-point drift could push beyond 1.
        double[] a = {1.0, 1.0, 1.0};
        double[] b = {1.0, 1.0, 1.0};
        double cos = CosineCalculator.cosine(a, b);
        assertThat(cos).isLessThanOrEqualTo(1.0).isGreaterThanOrEqualTo(-1.0);
    }
}
