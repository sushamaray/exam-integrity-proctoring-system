package com.uiu.examintegrity.proctoring.domain;

import java.util.List;

public record AggregateNode(String label, String kind, List<RiskNode> children, double multiplier)
        implements RiskNode {
    public AggregateNode {
        children = List.copyOf(children);
    }

    @Override
    public double score() {
        return Math.min(100, children.stream().mapToDouble(RiskNode::score).sum() * multiplier);
    }
}
