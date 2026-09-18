package com.uiu.examintegrity.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class ExamSession {
    private final Long id;
    private final Long examId;
    private final Long userId;

    private SessionState state;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant submittedAt;
    private Paper paper;

    private ExamSession(Long id, Long examId, Long userId, SessionState state,
            Instant startedAt, Instant expiresAt, Instant submittedAt, Paper paper) {
        this.id = id;
        this.examId = Objects.requireNonNull(examId);
        this.userId = Objects.requireNonNull(userId);
        this.state = Objects.requireNonNull(state);
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.submittedAt = submittedAt;
        this.paper = paper;
    }

    public static ExamSession allocate(Long examId, Long userId) {
        return new ExamSession(null, examId, userId, new NotStarted(), null, null, null, null);
    }

    public static ExamSession restore(Long id, Long examId, Long userId, SessionState state,
            Instant startedAt, Instant expiresAt, Instant submittedAt, Paper paper) {
        return new ExamSession(id, examId, userId, state, startedAt, expiresAt, submittedAt, paper);
    }

    public void start(Instant now, Duration allowance, Instant windowClosesAt, Paper drawn) {
        state = state.start();
        startedAt = now;
        Instant ownDeadline = now.plus(allowance);
        expiresAt = ownDeadline.isBefore(windowClosesAt) ? ownDeadline : windowClosesAt;
        paper = Objects.requireNonNull(drawn);
    }

    public RecordedAnswer recordAnswer(Long questionId, AnswerValue value, Instant now) {
        if (!state.acceptsAnswers()) {
            throw new IllegalStateTransition(state.name(), "recordAnswer");
        }
        if (isExpired(now)) {
            throw new SessionExpired(expiresAt);
        }
        if (!paper.contains(questionId)) {
            throw new QuestionNotOnPaper(questionId);
        }

        boolean hasOptions = !paper.optionsFor(questionId).isEmpty();
        if (hasOptions != value.isChoice()) {
            throw new InvalidAnswer(hasOptions
                    ? "This question is answered by choosing an option"
                    : "This question is answered in text");
        }
        if (value.isChoice() && !paper.optionsFor(questionId).contains(value.optionLabel())) {
            throw new InvalidAnswer("Option " + value.optionLabel() + " is not one of this question's choices");
        }
        return new RecordedAnswer(id, questionId, value, now);
    }

    public void submit(Instant now) {
        state = state.submit();
        submittedAt = now;
    }

    public void autoSubmit(Instant now) {
        state = state.autoSubmit();
        submittedAt = expiresAt != null && expiresAt.isBefore(now) ? expiresAt : now;
    }

    public void invalidate() {
        state = state.invalidate();
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public long remainingSeconds(Instant now) {
        if (expiresAt == null || !state.acceptsAnswers()) {
            return 0;
        }
        return Math.max(0, Duration.between(now, expiresAt).toSeconds());
    }

    public Long id() {
        return id;
    }

    public Long examId() {
        return examId;
    }

    public Long userId() {
        return userId;
    }

    public SessionState state() {
        return state;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Paper paper() {
        return paper;
    }
}
