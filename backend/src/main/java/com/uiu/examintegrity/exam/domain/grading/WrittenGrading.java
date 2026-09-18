package com.uiu.examintegrity.exam.domain.grading;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.question.QuestionType;

public final class WrittenGrading implements GradingStrategy {
    @Override
    public boolean supports(QuestionType type) {
        return type == QuestionType.SHORT_ANSWER || type == QuestionType.ESSAY;
    }

    @Override
    public GradeResult grade(AnswerValue answer, GradableQuestion question) {
        return GradeResult.pendingReview();
    }
}
