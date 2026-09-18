package com.uiu.examintegrity.similarity.domain;

public record PairKey(Long a, Long b) {
    public PairKey {
        if (a == null || b == null || a >= b) {
            throw new IllegalArgumentException("A pair key needs a < b, got " + a + ", " + b);
        }
    }

    public static PairKey of(Long x, Long y) {
        if (x.equals(y)) {
            throw new IllegalArgumentException("A sitting cannot be paired with itself");
        }
        return x < y ? new PairKey(x, y) : new PairKey(y, x);
    }

    public static boolean swaps(Long x, Long y) {
        return x > y;
    }
}
