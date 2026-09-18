package com.uiu.examintegrity.exam.domain;

public sealed interface SessionState
        permits NotStarted, InProgress, Submitted, AutoSubmitted, Voided {
    String name();

    default SessionState start() {
        throw illegal("start");
    }

    default SessionState submit() {
        throw illegal("submit");
    }

    default SessionState autoSubmit() {
        throw illegal("autoSubmit");
    }

    default SessionState invalidate() {
        throw illegal("invalidate");
    }

    default boolean acceptsAnswers() {
        return false;
    }

    private IllegalStateTransition illegal(String operation) {
        return new IllegalStateTransition(name(), operation);
    }

    static SessionState of(String name) {
        return switch (name) {
            case "NOT_STARTED" -> new NotStarted();
            case "IN_PROGRESS" -> new InProgress();
            case "SUBMITTED" -> new Submitted();
            case "AUTO_SUBMITTED" -> new AutoSubmitted();
            case "VOIDED" -> new Voided();
            default -> throw new IllegalArgumentException("Unknown session state " + name);
        };
    }
}
