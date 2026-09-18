package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class FocusLossDetector implements Detector {
    private static final Duration LOOKBACK = Duration.ofMinutes(2);
    private static final Duration MIN_AWAY = Duration.ofSeconds(10);
    private static final int MIN_EPISODES = 3;

    @Override
    public String name() {
        return "FOCUS_LOSS";
    }

    @Override
    public double baseWeight() {
        return 0.30;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> recent = window.since(window.now().minus(LOOKBACK));

        Duration away = Duration.ZERO;
        int episodes = 0;
        List<Long> evidence = new ArrayList<>();
        ProctoringEvent firstLeave = null;
        ProctoringEvent leftAt = null;
        boolean pageHidden = false;
        Instant lastSeen = null;

        for (ProctoringEvent event : recent) {
            if (event.type().isLeave() && leftAt == null) {
                leftAt = event;
                pageHidden = event.type() == EventType.TAB_HIDDEN;
            } else if (event.type() == EventType.TAB_HIDDEN && leftAt != null) {
                pageHidden = true;
            } else if (event.type().isReturn() && leftAt != null) {
                if (!pageHidden) {
                    away = away.plus(Duration.between(leftAt.occurredAt(), event.occurredAt()));
                    episodes++;
                    evidence.add(leftAt.id());
                    evidence.add(event.id());
                    lastSeen = event.occurredAt();
                    if (firstLeave == null) {
                        firstLeave = leftAt;
                    }
                }
                leftAt = null;
            }
        }
        if (leftAt != null && !pageHidden) {
            long reported = leftAt.payloadLong("durationMs", -1);
            away = away.plus(reported >= 0
                    ? Duration.ofMillis(reported)
                    : Duration.between(leftAt.occurredAt(), window.now()));
            episodes++;
            evidence.add(leftAt.id());
            lastSeen = leftAt.occurredAt();
            if (firstLeave == null) {
                firstLeave = leftAt;
            }
        }
        if (firstLeave == null) {
            return Optional.empty();
        }

        if (away.compareTo(MIN_AWAY) < 0 && episodes < MIN_EPISODES) {
            return Optional.empty();
        }

        double confidence = Math.min(0.95, 0.4 + away.toSeconds() / 60.0 + 0.05 * Math.max(0, episodes - 1));
        return Optional.of(new Signal(name(), baseWeight(), confidence,
                lastSeen != null ? lastSeen : firstLeave.occurredAt(), firstLeave.questionId(), evidence));
    }
}
