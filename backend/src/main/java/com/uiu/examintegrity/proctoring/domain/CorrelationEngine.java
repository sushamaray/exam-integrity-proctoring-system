package com.uiu.examintegrity.proctoring.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CorrelationEngine {
    public static final Duration DEFAULT_MAX_SPAN = Duration.ofSeconds(60);
    public static final int DEFAULT_MIN_SIGNALS = 3;
    public static final double STEP = 0.15;

    private final Duration maxSpan;
    private final int minSignals;

    public CorrelationEngine() {
        this(DEFAULT_MAX_SPAN, DEFAULT_MIN_SIGNALS);
    }

    public CorrelationEngine(Duration maxSpan, int minSignals) {
        this.maxSpan = maxSpan;
        this.minSignals = minSignals;
    }

    public List<Cluster> findClusters(List<Signal> signals) {
        List<Signal> sorted = new ArrayList<>(signals);
        sorted.sort(Comparator.comparing(Signal::at));

        List<Cluster> clusters = new ArrayList<>();
        int start = 0;
        while (start < sorted.size()) {
            int end = start;
            while (end + 1 < sorted.size()
                    && !sorted.get(end + 1).at().isAfter(sorted.get(start).at().plus(maxSpan))) {
                end++;
            }
            int count = end - start + 1;
            if (count >= minSignals) {
                clusters.add(new Cluster(sorted.subList(start, end + 1)));
                start = end + 1;
            } else {
                start++;
            }
        }
        return clusters;
    }

    public double multiplierFor(Cluster cluster) {
        return 1 + STEP * (cluster.size() - minSignals + 1);
    }
}
