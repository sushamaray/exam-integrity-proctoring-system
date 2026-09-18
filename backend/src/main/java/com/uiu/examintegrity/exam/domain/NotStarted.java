package com.uiu.examintegrity.exam.domain;

public record NotStarted() implements SessionState {
    @Override
    public String name() {
        return "NOT_STARTED";
    }

    @Override
    public SessionState start() {
        return new InProgress();
    }
}
