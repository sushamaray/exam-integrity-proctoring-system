package com.uiu.examintegrity.proctoring.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionWindow {
    public static final Duration DEFAULT_RETENTION = Duration.ofMinutes(5);

    private static final Comparator<ProctoringEvent> BY_TIME =
            Comparator.comparing(ProctoringEvent::occurredAt).thenComparing(
                    ProctoringEvent::id, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final List<ProctoringEvent> events = new ArrayList<>();
    private final Set<Long> seenIds = new HashSet<>();
    private final List<Signal> signals = new ArrayList<>();

    public static SessionWindow of(ProctoringEvent... events) {
        SessionWindow window = new SessionWindow();
        for (ProctoringEvent event : events) {
            window.append(event);
        }
        return window;
    }

    public void append(ProctoringEvent event) {
        if (event.id() != null && !seenIds.add(event.id())) {
            return;
        }
        int at = Collections.binarySearch(events, event, BY_TIME);
        events.add(at < 0 ? -at - 1 : at, event);
    }

    public void prune(Duration retention) {
        Instant cutoff = now().minus(retention);
        events.removeIf(event -> event.occurredAt().isBefore(cutoff));
    }

    public Instant now() {
        return events.isEmpty() ? Instant.EPOCH : events.get(events.size() - 1).occurredAt();
    }

    public List<ProctoringEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public List<ProctoringEvent> recent(EventType type, Duration within) {
        return since(now().minus(within)).stream().filter(e -> e.type() == type).toList();
    }

    public List<ProctoringEvent> since(Instant from) {
        return events.stream().filter(e -> !e.occurredAt().isBefore(from)).toList();
    }

    public boolean recordSignal(Signal signal) {
        Signal merged = signal;
        List<Signal> overlapping = new ArrayList<>();
        for (Signal existing : signals) {
            if (existing.detector().equals(signal.detector())
                    && !Collections.disjoint(existing.eventIds(), signal.eventIds())) {
                if (existing.eventIds().containsAll(signal.eventIds())
                        && existing.confidence() >= signal.confidence()) {
                    return false;
                }
                overlapping.add(existing);
                merged = merge(merged, existing);
            }
        }
        signals.removeAll(overlapping);
        signals.add(merged);
        signals.sort(Comparator.comparing(Signal::at));
        return true;
    }

    private static Signal merge(Signal newer, Signal older) {
        Set<Long> ids = new LinkedHashSet<>(older.eventIds());
        ids.addAll(newer.eventIds());
        Instant at = newer.at().isAfter(older.at()) ? newer.at() : older.at();
        return new Signal(newer.detector(), newer.weight(), Math.max(newer.confidence(), older.confidence()),
                at, newer.questionId() != null ? newer.questionId() : older.questionId(), List.copyOf(ids));
    }

    public List<Signal> signals() {
        return Collections.unmodifiableList(signals);
    }
}
