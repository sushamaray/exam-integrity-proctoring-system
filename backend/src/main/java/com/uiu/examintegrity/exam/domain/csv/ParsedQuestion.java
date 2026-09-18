package com.uiu.examintegrity.exam.domain.csv;

import java.math.BigDecimal;
import java.util.List;

import com.uiu.examintegrity.question.QuestionType;

public record ParsedQuestion(QuestionType type, String topic, String question, List<String> options,
        String correctLabel, BigDecimal marks) {
    public boolean isWritten() {
        return type == QuestionType.SHORT_ANSWER;
    }
}
