package com.uiu.examintegrity.exam.domain.grading;

import java.math.BigDecimal;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.question.QuestionType;

public final class McqGrading implements GradingStrategy {
    @Override
    public boolean supports(QuestionType type) {
        return type == QuestionType.MCQ;
    }

    @Override
    public GradeResult grade(AnswerValue answer, GradableQuestion question) {
        boolean correct = answer.isChoice() && answer.optionLabel().equals(question.correctLabel());
        return GradeResult.graded(correct ? question.marks() : BigDecimal.ZERO);
    }
}
