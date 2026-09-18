package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class TabSwitchDetector implements Detector {
    private static final Duration LOOKBACK = Duration.ofMinutes(2);

    @Override
    public String name() {
        return "TAB_SWITCH";
    }

    @Override
    public double baseWeight() {
        return 0.50;
    }

    @Override
    public boolean isRuleViolation() {
        return true;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> recent = window.since(window.now().minus(LOOKBACK));

        ProctoringEvent hidden = null;
        for (ProctoringEvent event : recent) {
            if (event.type() == EventType.TAB_HIDDEN) {
                hidden = event;
            }
        }
        if (hidden == null) {
            return Optional.empty();
        }

        ProctoringEvent back = null;
        for (ProctoringEvent event : recent) {
            if (event.type() == EventType.TAB_VISIBLE && event.occurredAt().isAfter(hidden.occurredAt())) {
                back = event;
                break;
            }
        }

        Duration away = Duration.between(hidden.occurredAt(), back != null ? back.occurredAt() : window.now());
        double confidence = away.compareTo(Duration.ofSeconds(30)) >= 0 ? 1.0
                : away.compareTo(Duration.ofSeconds(5)) >= 0 ? 0.9
                : 0.8;

        List<Long> evidence = new ArrayList<>();
        evidence.add(hidden.id());
        if (back != null) {
            evidence.add(back.id());
        }
        return Optional.of(new Signal(name(), baseWeight(), confidence, hidden.occurredAt(),
                hidden.questionId(), evidence));
    }
}
