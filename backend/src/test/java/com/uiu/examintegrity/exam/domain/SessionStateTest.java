package com.uiu.examintegrity.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionStateTest {
    @Test
    @DisplayName("the happy path: not started, in progress, submitted")
    void happyPath() {
        SessionState state = new NotStarted().start();
        assertThat(state).isInstanceOf(InProgress.class);
        assertThat(state.acceptsAnswers()).isTrue();
        assertThat(state.submit()).isInstanceOf(Submitted.class);
        assertThat(state.autoSubmit()).isInstanceOf(AutoSubmitted.class);
    }

    @Test
    @DisplayName("only an in-progress session accepts answers")
    void onlyInProgressAcceptsAnswers() {
        List<SessionState> closed = List.of(new NotStarted(), new Submitted(), new AutoSubmitted(), new Voided());
        assertThat(closed).noneMatch(SessionState::acceptsAnswers);
    }

    @Test
    @DisplayName("a submitted session cannot be submitted, auto-submitted or restarted")
    void submittedIsFinal() {
        SessionState submitted = new Submitted();
        assertThatThrownBy(submitted::submit).isInstanceOf(IllegalStateTransition.class)
                .hasMessageContaining("SUBMITTED");
        assertThatThrownBy(submitted::autoSubmit).isInstanceOf(IllegalStateTransition.class);
        assertThatThrownBy(submitted::start).isInstanceOf(IllegalStateTransition.class);
    }

    @Test
    @DisplayName("a session that never started cannot be submitted or voided")
    void notStartedCannotClose() {
        assertThatThrownBy(new NotStarted()::submit).isInstanceOf(IllegalStateTransition.class);
        assertThatThrownBy(new NotStarted()::invalidate).isInstanceOf(IllegalStateTransition.class);
    }

    @Test
    @DisplayName("a sitting that ran can be voided, and a voided one can do nothing")
    void voiding() {
        assertThat(new InProgress().invalidate()).isInstanceOf(Voided.class);
        assertThat(new Submitted().invalidate()).isInstanceOf(Voided.class);
        assertThat(new AutoSubmitted().invalidate()).isInstanceOf(Voided.class);

        SessionState voided = new Voided();
        assertThatThrownBy(voided::start).isInstanceOf(IllegalStateTransition.class);
        assertThatThrownBy(voided::submit).isInstanceOf(IllegalStateTransition.class);
        assertThatThrownBy(voided::invalidate).isInstanceOf(IllegalStateTransition.class);
    }

    @Test
    @DisplayName("names round-trip through of(), matching the database CHECK values")
    void namesRoundTrip() {
        for (String name : List.of("NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "AUTO_SUBMITTED", "VOIDED")) {
            assertThat(SessionState.of(name).name()).isEqualTo(name);
        }
        assertThatThrownBy(() -> SessionState.of("PAUSED")).isInstanceOf(IllegalArgumentException.class);
    }
}
