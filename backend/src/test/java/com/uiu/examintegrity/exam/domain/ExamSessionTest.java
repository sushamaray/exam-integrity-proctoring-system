package com.uiu.examintegrity.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExamSessionTest {
    private static final Instant T0 = Instant.parse("2026-09-18T09:00:00Z");
    private static final Instant WINDOW_CLOSES = T0.plus(Duration.ofHours(3));

    private static final Paper PAPER = new Paper(List.of(1L, 2L),
            Map.of(1L, List.of("c", "a", "d", "b")));

    private ExamSession started() {
        ExamSession session = ExamSession.restore(10L, 5L, 7L, new NotStarted(), null, null, null, null);
        session.start(T0, Duration.ofMinutes(60), WINDOW_CLOSES, PAPER);
        return session;
    }

    @Test
    @DisplayName("the deadline is start plus the allowance")
    void deadlineFromAllowance() {
        ExamSession session = started();
        assertThat(session.expiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(60)));
        assertThat(session.remainingSeconds(T0.plusSeconds(90))).isEqualTo(3600 - 90);
    }

    @Test
    @DisplayName("starting late in the window buys only what is left of the window")
    void deadlineCappedByWindow() {
        ExamSession session = ExamSession.allocate(5L, 7L);
        Instant late = WINDOW_CLOSES.minus(Duration.ofMinutes(10));
        session.start(late, Duration.ofMinutes(60), WINDOW_CLOSES, PAPER);
        assertThat(session.expiresAt()).isEqualTo(WINDOW_CLOSES);
    }

    @Test
    @DisplayName("an answer inside the time is accepted")
    void acceptsAnswer() {
        RecordedAnswer answer = started().recordAnswer(1L, AnswerValue.choice("a"), T0.plusSeconds(30));
        assertThat(answer.questionId()).isEqualTo(1L);
        assertThat(answer.at()).isEqualTo(T0.plusSeconds(30));
    }

    @Test
    @DisplayName("an answer after the deadline is refused, whatever the client's countdown said")
    void refusesAfterDeadline() {
        ExamSession session = started();
        assertThatThrownBy(() -> session.recordAnswer(1L, AnswerValue.choice("a"), session.expiresAt()))
                .isInstanceOf(SessionExpired.class);
        assertThat(session.remainingSeconds(session.expiresAt().plusSeconds(5))).isZero();
    }

    @Test
    @DisplayName("saving to a submitted session is impossible, not merely discouraged")
    void refusesAfterSubmit() {
        ExamSession session = started();
        session.submit(T0.plusSeconds(600));
        assertThatThrownBy(() -> session.recordAnswer(1L, AnswerValue.choice("a"), T0.plusSeconds(601)))
                .isInstanceOf(IllegalStateTransition.class);
        assertThat(session.remainingSeconds(T0.plusSeconds(601))).isZero();
    }

    @Test
    @DisplayName("questions and options not on this paper are refused")
    void refusesForeignQuestionsAndOptions() {
        ExamSession session = started();
        assertThatThrownBy(() -> session.recordAnswer(99L, AnswerValue.choice("a"), T0))
                .isInstanceOf(QuestionNotOnPaper.class);
        assertThatThrownBy(() -> session.recordAnswer(1L, AnswerValue.choice("e"), T0))
                .isInstanceOf(InvalidAnswer.class);
    }

    @Test
    @DisplayName("an answer must have the shape its question expects")
    void refusesWrongShape() {
        ExamSession session = started();
        assertThatThrownBy(() -> session.recordAnswer(1L, AnswerValue.text("a guess"), T0))
                .isInstanceOf(InvalidAnswer.class);
        assertThatThrownBy(() -> session.recordAnswer(2L, AnswerValue.choice("a"), T0))
                .isInstanceOf(InvalidAnswer.class);
        assertThat(session.recordAnswer(2L, AnswerValue.text("Observer decouples"), T0)).isNotNull();
    }

    @Test
    @DisplayName("an auto-submit is stamped at the deadline, not when the sweep got round to it")
    void autoSubmitStampedAtDeadline() {
        ExamSession session = started();
        Instant sweep = session.expiresAt().plusSeconds(27);
        session.autoSubmit(sweep);
        assertThat(session.state()).isInstanceOf(AutoSubmitted.class);
        assertThat(session.submittedAt()).isEqualTo(session.expiresAt());
    }

    @Test
    @DisplayName("a second submit is refused")
    void doubleSubmit() {
        ExamSession session = started();
        session.submit(T0.plusSeconds(10));
        assertThatThrownBy(() -> session.submit(T0.plusSeconds(11))).isInstanceOf(IllegalStateTransition.class);
        assertThatThrownBy(() -> session.autoSubmit(T0.plusSeconds(11))).isInstanceOf(IllegalStateTransition.class);
    }

    @Test
    @DisplayName("an answer value is an option or text, never both or neither")
    void answerValueShape() {
        assertThatThrownBy(() -> new AnswerValue("a", "text")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnswerValue(null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
