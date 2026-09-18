package com.uiu.examintegrity.similarity.application.view;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GraphView(
        Long examId,
        String title,
        String courseCode,
        Instant computedAt,
        Map<String, Double> thresholds,
        List<Node> nodes,
        List<Edge> edges) {
    public record Node(Long sessionId, String student, String studentId, Integer cluster) {
    }

    public record Edge(Long pairId, Long a, Long b, double score, String method, Long questionId,
            String questionText, String detail) {
    }
}
