package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class LargePasteDetector implements Detector {
    static final int THRESHOLD = 120;
    private static final Duration LOOKBACK = Duration.ofSeconds(30);

    @Override
    public String name() {
        return "LARGE_PASTE";
    }

    @Override
    public double baseWeight() {
        return 0.45;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> large = window.recent(EventType.PASTE, LOOKBACK).stream()
                .filter(e -> e.payloadInt("charCount") >= THRESHOLD)
                .toList();
        if (large.isEmpty()) {
            return Optional.empty();
        }

        ProctoringEvent biggest = large.stream()
                .max((a, b) -> Integer.compare(a.payloadInt("charCount"), b.payloadInt("charCount")))
                .orElseThrow();
        ProctoringEvent latest = large.get(large.size() - 1);

        return Optional.of(new Signal(name(), baseWeight(), confidenceFor(biggest.payloadInt("charCount")),
                latest.occurredAt(), latest.questionId(), large.stream().map(ProctoringEvent::id).toList()));
    }

    static double confidenceFor(int chars) {
        return Math.min(0.99, 0.6 + (chars - THRESHOLD) / 400.0);
    }
}
