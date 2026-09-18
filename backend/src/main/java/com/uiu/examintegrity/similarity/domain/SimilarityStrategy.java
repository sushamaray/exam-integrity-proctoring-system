package com.uiu.examintegrity.similarity.domain;

import java.util.List;

import com.uiu.examintegrity.question.QuestionType;

public interface SimilarityStrategy {
    String method();

    boolean supports(QuestionType type);

    double threshold();

    PairScorer prepare(List<SessionAnswers> cohort);

    @FunctionalInterface
    interface PairScorer {
        List<PairResult> score(SessionAnswers a, SessionAnswers b);
    }
}
