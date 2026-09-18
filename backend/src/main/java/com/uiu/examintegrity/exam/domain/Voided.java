package com.uiu.examintegrity.exam.domain;

public record Voided() implements SessionState {
    @Override
    public String name() {
        return "VOIDED";
    }
}
