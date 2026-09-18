package com.uiu.examintegrity.exam.domain.grading;

import java.math.BigDecimal;

import com.uiu.examintegrity.question.QuestionType;

public record GradableQuestion(Long id, QuestionType type, String correctLabel, BigDecimal marks) {
}
