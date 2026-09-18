package com.uiu.examintegrity.proctoring.application;

import java.util.List;
import java.util.Map;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;

final class ScoringContext {
    private final Long sessionId;
    private final Long examId;
    private final SessionWindow window = new SessionWindow();
    private final List<Detector> detectors;
    private final Map<Long, Integer> positionOnPaper;

    ScoringContext(Long sessionId, Long examId, List<Detector> detectors, Map<Long, Integer> positionOnPaper) {
        this.sessionId = sessionId;
        this.examId = examId;
        this.detectors = List.copyOf(detectors);
        this.positionOnPaper = Map.copyOf(positionOnPaper);
    }

    void feed(ProctoringEvent event) {
        window.append(event);
        window.prune(SessionWindow.DEFAULT_RETENTION);
        for (Detector detector : detectors) {
            detector.evaluate(window).ifPresent(window::recordSignal);
        }
    }

    String questionLabel(Long questionId) {
        if (questionId == null) {
            return "Not tied to a question";
        }
        Integer position = positionOnPaper.get(questionId);
        return position == null ? "Question " + questionId : "Q" + position;
    }

    Long sessionId() {
        return sessionId;
    }

    Long examId() {
        return examId;
    }

    SessionWindow window() {
        return window;
    }
}
