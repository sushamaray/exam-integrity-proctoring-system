package com.uiu.examintegrity.exam.domain.grading;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.exam.domain.AnswerValue;
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
    @DisplayName("written and coding answers wait for a human, with no provisional zero")
    void pendingForHumans() {
        GradableQuestion essay = new GradableQuestion(2L, QuestionType.ESSAY, null, TWO);
        GradableQuestion code = new GradableQuestion(3L, QuestionType.CODING, null, TWO);

        GradeResult written = new WrittenGrading().grade(AnswerValue.text("..."), essay);
        GradeResult coding = new CodingGrading().grade(AnswerValue.text("int main"), code);

        assertThat(written.pending()).isTrue();
        assertThat(written.marks()).isNull();
        assertThat(coding.pending()).isTrue();
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
