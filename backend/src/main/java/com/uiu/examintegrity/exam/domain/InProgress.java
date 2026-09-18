package com.uiu.examintegrity.exam.domain;

public record InProgress() implements SessionState {
    @Override
    public String name() {
        return "IN_PROGRESS";
    }

    @Override
    public SessionState submit() {
        return new Submitted();
    }

    @Override
    public SessionState autoSubmit() {
        return new AutoSubmitted();
    }

    @Override
    public SessionState invalidate() {
        return new Voided();
    }

    @Override
    public boolean acceptsAnswers() {
        return true;
    }
}
