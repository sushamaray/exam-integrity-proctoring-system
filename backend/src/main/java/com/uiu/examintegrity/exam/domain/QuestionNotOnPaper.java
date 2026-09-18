package com.uiu.examintegrity.exam.domain;

public class QuestionNotOnPaper extends RuntimeException {
    public QuestionNotOnPaper(Long questionId) {
        super("Question " + questionId + " is not on this paper");
    }
}
