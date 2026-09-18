package com.uiu.examintegrity.similarity.domain;

import java.util.List;

public record PairResult(PairKey pair, Long questionId, String method, double score, List<Span> spans,
        List<Long> matchedQuestionIds, String detail) {
    public PairResult {
        spans = spans == null ? List.of() : List.copyOf(spans);
        matchedQuestionIds = matchedQuestionIds == null ? List.of() : List.copyOf(matchedQuestionIds);
    }
}
