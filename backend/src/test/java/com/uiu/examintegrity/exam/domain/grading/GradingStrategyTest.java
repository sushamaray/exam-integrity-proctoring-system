package com.uiu.examintegrity.exam.domain.grading;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.exam.domain.grading.GradableQuestion.CodingOutcome;
import com.uiu.examintegrity.question.QuestionType;

class GradingStrategyTest {
    private static final BigDecimal TWO = new BigDecimal("2.00");

    @Test
    @DisplayName("MCQ: the right option earns the question's marks, a wrong one earns zero")
    void mcq() {
        GradableQuestion question = new GradableQuestion(1L, QuestionType.MCQ, "c", TWO);
        McqGrading grading = new McqGrading();

        assertThat(grading.grade(AnswerValue.choice("c"), question)).isEqualTo(GradeResult.graded(TWO));
        assertThat(grading.grade(AnswerValue.choice("a"), question).marks()).isEqualByComparingTo("0");
        assertThat(grading.grade(AnswerValue.choice("a"), question).pending()).isFalse();
    }

    @Test
    @DisplayName("a written answer waits for a human, with no provisional zero")
    void pendingForHumans() {
        GradableQuestion essay = new GradableQuestion(2L, QuestionType.ESSAY, null, TWO);

        GradeResult written = new WrittenGrading().grade(AnswerValue.text("..."), essay);

        assertThat(written.pending()).isTrue();
        assertThat(written.marks()).isNull();
    }

    @Test
    @DisplayName("coding earns marks in proportion to the cases its final submission passed")
    void codingIsPartialCredit() {
        BigDecimal ten = new BigDecimal("10.00");
        CodingGrading grading = new CodingGrading();

        assertThat(coding(grading, ten, 4, 4).marks()).isEqualByComparingTo("10.00");
        assertThat(coding(grading, ten, 3, 4).marks()).isEqualByComparingTo("7.50");
        assertThat(coding(grading, ten, 1, 3).marks()).isEqualByComparingTo("3.33");
        assertThat(coding(grading, ten, 0, 4).marks()).isEqualByComparingTo("0.00");
        assertThat(coding(grading, ten, 3, 4).pending()).isFalse();
    }

    @Test
    @DisplayName("a student who never submitted a run is left for a human, not given a zero")
    void codingWithoutASubmissionStaysPending() {
        CodingGrading grading = new CodingGrading();

        GradeResult never = grading.grade(AnswerValue.text("int main(void) {}"),
                new GradableQuestion(3L, QuestionType.CODING, null, TWO));
        GradeResult noCases = grading.grade(AnswerValue.text("int main(void) {}"),
                new GradableQuestion(3L, QuestionType.CODING, null, TWO, new CodingOutcome(0, 0)));

        assertThat(never.pending()).isTrue();
        assertThat(never.marks()).isNull();
        assertThat(noCases.pending()).isTrue();
    }

    private static GradeResult coding(CodingGrading grading, BigDecimal marks, int passed, int total) {
        return grading.grade(AnswerValue.text("int main(void) {}"),
                new GradableQuestion(3L, QuestionType.CODING, null, marks, new CodingOutcome(passed, total)));
    }

    @Test
    @DisplayName("every question type is covered by exactly one strategy")
    void everyTypeCoveredOnce() {
        var strategies = java.util.List.of(new McqGrading(), new WrittenGrading(), new CodingGrading());
        for (QuestionType type : QuestionType.values()) {
            assertThat(strategies.stream().filter(s -> s.supports(type)).count())
                    .as("strategies supporting %s", type)
                    .isEqualTo(1);
        }
    }
}
