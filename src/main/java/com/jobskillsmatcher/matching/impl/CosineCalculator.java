package com.jobskillsmatcher.matching.impl;


public final class CosineCalculator {

    private CosineCalculator() { }

    public static double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vectors must be the same length: a=" + a.length + " b=" + b.length);
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na <= 0.0 || nb <= 0.0) {
            return 0.0;
        }
        double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));
        // Clamp against float rounding drift.
        if (cos < -1.0) return -1.0;
        if (cos > 1.0) return 1.0;
        return cos;
    }
}
