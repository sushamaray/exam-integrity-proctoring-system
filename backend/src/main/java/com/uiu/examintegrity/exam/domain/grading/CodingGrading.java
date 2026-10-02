package com.uiu.examintegrity.exam.domain.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.exam.domain.grading.GradableQuestion.CodingOutcome;
import com.uiu.examintegrity.question.QuestionType;

/**
 * Awards marks in proportion to the hidden test cases a submission passed.
 *
 * <p>The tests were already run when the student pressed Submit on the question, so this only
 * reads the tally. That is not an optimisation: grading runs inside a pessimistic lock on the
 * session row, and compiling there would block the student's own autosave.
 *
 * <p>With no final submission the result stays pending for a human, which is what this
 * strategy did for every coding answer before test cases existed.
 */
public final class CodingGrading implements GradingStrategy {
    @Override
    public boolean supports(QuestionType type) {
        return type == QuestionType.CODING;
    }

    @Override
    public GradeResult grade(AnswerValue answer, GradableQuestion question) {
        CodingOutcome outcome = question.coding();
        if (outcome == null || !outcome.decided()) {
            return GradeResult.pendingReview();
        }
        if (outcome.passed() <= 0) {
            return GradeResult.graded(BigDecimal.ZERO.setScale(2));
        }

        BigDecimal earned = question.marks()
                .multiply(BigDecimal.valueOf(outcome.passed()))
                .divide(BigDecimal.valueOf(outcome.total()), 2, RoundingMode.HALF_UP);
        return GradeResult.graded(earned);
    }
}
