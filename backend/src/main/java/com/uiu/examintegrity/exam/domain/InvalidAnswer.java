package com.uiu.examintegrity.exam.domain;

public class InvalidAnswer extends RuntimeException {
    public InvalidAnswer(String message) {
        super(message);
    }
}
