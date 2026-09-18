package com.uiu.examintegrity.proctoring.domain;

public enum RiskTier {
    CLEAR,
    REVIEW,
    ESCALATE;

    private static final int ESCALATE_ABOVE = 60;
    private static final int REVIEW_ABOVE = 25;

    public static RiskTier of(double score) {
        if (score > ESCALATE_ABOVE) {
            return ESCALATE;
        }
        return score > REVIEW_ABOVE ? REVIEW : CLEAR;
    }
}
