package com.uiu.examintegrity.exam.domain;

public class IllegalStateTransition extends RuntimeException {
    private final String state;
    private final String operation;

    public IllegalStateTransition(String state, String operation) {
        super("Cannot " + operation + " a session that is " + state);
        this.state = state;
        this.operation = operation;
    }

    public String state() {
        return state;
    }

    public String operation() {
        return operation;
    }
}
