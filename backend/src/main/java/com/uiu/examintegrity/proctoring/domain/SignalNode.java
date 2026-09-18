package com.uiu.examintegrity.proctoring.domain;

public record SignalNode(String label, Signal signal) implements RiskNode {
    @Override
    public double score() {
        return signal.weight() * signal.confidence() * 100;
    }
}
