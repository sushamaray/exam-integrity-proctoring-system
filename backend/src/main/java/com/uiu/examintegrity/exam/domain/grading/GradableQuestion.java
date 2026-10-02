package com.uiu.examintegrity.exam.domain.grading;

import java.math.BigDecimal;

import com.uiu.examintegrity.question.QuestionType;

/**
 * Everything a {@link GradingStrategy} needs about one question, assembled per sitting.
 *
 * @param coding how the student's final run went, or null for any type that is not CODING
 */
public record GradableQuestion(Long id, QuestionType type, String correctLabel, BigDecimal marks,
        CodingOutcome coding) {

    /** How many of a coding question's cases the final submission passed. */
    public record CodingOutcome(int passed, int total) {
        public boolean decided() {
            return total > 0;
        }
    }

    /** For the types that have no code behind them. */
    public GradableQuestion(Long id, QuestionType type, String correctLabel, BigDecimal marks) {
        this(id, type, correctLabel, marks, null);
    }
}
