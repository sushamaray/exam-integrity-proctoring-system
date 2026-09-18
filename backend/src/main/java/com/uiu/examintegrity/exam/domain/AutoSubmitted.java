package com.uiu.examintegrity.exam.domain;

public record AutoSubmitted() implements SessionState {
    @Override
    public String name() {
        return "AUTO_SUBMITTED";
    }

    @Override
    public SessionState invalidate() {
        return new Voided();
    }
}
