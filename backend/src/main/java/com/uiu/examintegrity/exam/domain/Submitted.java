package com.uiu.examintegrity.exam.domain;

public record Submitted() implements SessionState {
    @Override
    public String name() {
        return "SUBMITTED";
    }

    @Override
    public SessionState invalidate() {
        return new Voided();
    }
}
