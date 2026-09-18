package com.uiu.examintegrity.similarity.domain;

import java.util.List;

public record SimilarityReport(Long examId, List<Node> nodes, List<PairResult> edges) {
    public record Node(Long sessionId, String student, Integer cluster) {
    }
}
