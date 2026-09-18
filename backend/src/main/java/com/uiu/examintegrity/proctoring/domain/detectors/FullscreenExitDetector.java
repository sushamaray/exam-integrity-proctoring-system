package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class FullscreenExitDetector implements Detector {
    private static final Duration LOOKBACK = Duration.ofSeconds(60);

    @Override
    public String name() {
        return "FULLSCREEN_EXIT";
    }

    @Override
    public double baseWeight() {
        return 0.25;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> exits = window.recent(EventType.FS_EXIT, LOOKBACK);
        if (exits.isEmpty()) {
            return Optional.empty();
        }
        ProctoringEvent latest = exits.get(exits.size() - 1);
        double confidence = Math.min(0.9, 0.5 + 0.15 * (exits.size() - 1));
        return Optional.of(new Signal(name(), baseWeight(), confidence, latest.occurredAt(),
                latest.questionId(), exits.stream().map(ProctoringEvent::id).toList()));
    }
}
