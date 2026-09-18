package com.uiu.examintegrity.proctoring.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.uiu.examintegrity.proctoring.domain.AggregateNode;
import com.uiu.examintegrity.proctoring.domain.RiskNode;
import com.uiu.examintegrity.proctoring.domain.SignalNode;

public final class RiskBreakdown {
    private RiskBreakdown() {
    }

    public static List<Map<String, Object>> flatten(AggregateNode root) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RiskNode child : root.children()) {
            walk(child, 0, rows);
        }
        return rows;
    }

    private static void walk(RiskNode node, int depth, List<Map<String, Object>> rows) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", node.label());
        row.put("depth", depth);
        row.put("score", round(node.score()));

        switch (node) {
            case SignalNode leaf -> {
                row.put("kind", "SIGNAL");
                row.put("detector", leaf.signal().detector());
                row.put("weight", leaf.signal().weight());
                row.put("confidence", round3(leaf.signal().confidence()));
                row.put("at", leaf.signal().at().toString());
                row.put("questionId", leaf.signal().questionId());
                row.put("eventIds", leaf.signal().eventIds());
                rows.add(row);
            }
            case AggregateNode group -> {
                row.put("kind", group.kind());
                row.put("multiplier", group.multiplier());
                List<Instant> times = group.children().stream()
                        .filter(SignalNode.class::isInstance)
                        .map(child -> ((SignalNode) child).signal().at())
                        .sorted()
                        .toList();
                if (!times.isEmpty()) {
                    row.put("start", times.get(0).toString());
                    row.put("end", times.get(times.size() - 1).toString());
                }
                rows.add(row);
                for (RiskNode child : group.children()) {
                    walk(child, depth + 1, rows);
                }
            }
        }
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal round3(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }
}
