package com.uiu.examintegrity.proctoring.domain.detectors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.SessionWindow;
import com.uiu.examintegrity.proctoring.domain.Signal;

public final class IdleThenBurstDetector implements Detector {
    static final Duration MIN_IDLE = Duration.ofSeconds(90);
    static final Duration BURST_SPAN = Duration.ofSeconds(20);
    static final int MIN_BURST = 3;

    @Override
    public String name() {
        return "IDLE_THEN_BURST";
    }

    @Override
    public double baseWeight() {
        return 0.35;
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        List<ProctoringEvent> answers = window.events().stream()
                .filter(e -> e.type() == EventType.ANSWER)
                .toList();
        if (answers.size() < MIN_BURST) {
            return Optional.empty();
        }

        ProctoringEvent newest = answers.get(answers.size() - 1);
        int start = answers.size() - 1;
        while (start > 0 && !answers.get(start - 1).occurredAt()
                .isBefore(newest.occurredAt().minus(BURST_SPAN))) {
            start--;
        }
        List<ProctoringEvent> burst = answers.subList(start, answers.size());

        Set<Long> questions = new HashSet<>();
        burst.forEach(e -> questions.add(e.questionId()));
        if (questions.size() < MIN_BURST) {
            return Optional.empty();
        }

        var idleFrom = start > 0 ? answers.get(start - 1).occurredAt() : window.events().get(0).occurredAt();
        Duration idle = Duration.between(idleFrom, burst.get(0).occurredAt());
        if (idle.compareTo(MIN_IDLE) < 0) {
            return Optional.empty();
        }

        double confidence = Math.min(0.9, 0.6 + 0.1 * (questions.size() - MIN_BURST));
        List<Long> evidence = new ArrayList<>(burst.stream().map(ProctoringEvent::id).toList());
        return Optional.of(new Signal(name(), baseWeight(), confidence, newest.occurredAt(),
                newest.questionId(), evidence));
    }
}
