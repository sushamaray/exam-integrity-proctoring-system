package com.uiu.examintegrity.exam.domain.grading;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.question.QuestionType;

public interface GradingStrategy {
    boolean supports(QuestionType type);

    GradeResult grade(AnswerValue answer, GradableQuestion question);
}
