package com.uiu.examintegrity.proctoring.domain;

import java.time.Instant;
import java.util.List;

public record Cluster(List<Signal> signals) {
    public Cluster {
        signals = List.copyOf(signals);
        if (signals.isEmpty()) {
            throw new IllegalArgumentException("A cluster needs signals");
        }
    }

    public Instant start() {
        return signals.get(0).at();
    }

    public Instant end() {
        return signals.get(signals.size() - 1).at();
    }

    public int size() {
        return signals.size();
    }
}
