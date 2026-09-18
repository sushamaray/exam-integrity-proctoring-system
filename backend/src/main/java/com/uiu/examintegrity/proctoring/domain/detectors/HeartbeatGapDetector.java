package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class HeartbeatGapDetector implements Detector {
    static final Duration MAX_GAP = Duration.ofSeconds(15);

    @Override
    public String name() {
        return "HEARTBEAT_GAP";
    }

    @Override
    public double baseWeight() {
        return 0.20;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> beats = window.events().stream()
                .filter(e -> e.type() == EventType.HEARTBEAT)
                .toList();

        for (int i = beats.size() - 1; i > 0; i--) {
            Duration gap = Duration.between(beats.get(i - 1).occurredAt(), beats.get(i).occurredAt());
            if (gap.compareTo(MAX_GAP) > 0) {
                ProctoringEvent resumed = beats.get(i);
                double confidence = Math.min(0.9, 0.3 + gap.toSeconds() / 60.0);
                return Optional.of(new Signal(name(), baseWeight(), confidence, resumed.occurredAt(),
                        resumed.questionId(), List.of(beats.get(i - 1).id(), resumed.id())));
            }
        }
        return Optional.empty();
    }
}
