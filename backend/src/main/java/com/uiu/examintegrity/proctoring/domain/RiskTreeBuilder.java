package com.uiu.examintegrity.proctoring.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class RiskTreeBuilder {
    public static final double ISOLATED_DISCOUNT = 0.5;

    private final CorrelationEngine correlation;
    private final Set<String> ruleViolations;

    public RiskTreeBuilder(CorrelationEngine correlation) {
        this(correlation, Set.of());
    }

    public RiskTreeBuilder(CorrelationEngine correlation, Set<String> ruleViolations) {
        this.correlation = correlation;
        this.ruleViolations = Set.copyOf(ruleViolations);
    }

    public AggregateNode build(List<Signal> signals, List<Cluster> clusters,
            Function<Long, String> questionLabel) {
        List<RiskNode> children = new ArrayList<>();
        Set<Signal> clustered = new HashSet<>();

        for (Cluster cluster : clusters) {
            List<RiskNode> members = new ArrayList<>();
            for (Signal signal : cluster.signals()) {
                members.add(new SignalNode(signal.detector(), signal));
                clustered.add(signal);
            }
            children.add(new AggregateNode("Correlated cluster", "CLUSTER", members,
                    correlation.multiplierFor(cluster)));
        }

        List<RiskNode> breaches = new ArrayList<>();
        Map<Long, List<RiskNode>> byQuestion = new LinkedHashMap<>();
        for (Signal signal : signals) {
            if (clustered.contains(signal)) {
                continue;
            }
            SignalNode node = new SignalNode(signal.detector(), signal);
            if (ruleViolations.contains(signal.detector())) {
                breaches.add(node);
            } else {
                byQuestion.computeIfAbsent(signal.questionId(), q -> new ArrayList<>()).add(node);
            }
        }
        if (!breaches.isEmpty()) {
            children.add(new AggregateNode("Exam rule broken", "RULE", breaches, 1.0));
        }
        byQuestion.forEach((questionId, nodes) -> children.add(new AggregateNode(
                questionLabel.apply(questionId), "QUESTION", nodes, ISOLATED_DISCOUNT)));

        return new AggregateNode("Session", "SESSION", children, 1.0);
    }
}
