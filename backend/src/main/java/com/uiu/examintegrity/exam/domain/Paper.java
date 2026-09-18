package com.uiu.examintegrity.exam.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Paper(List<Long> questionOrder, Map<Long, List<String>> optionOrder) {
    public Paper {
        questionOrder = List.copyOf(questionOrder);
        Map<Long, List<String>> copy = new LinkedHashMap<>();
        optionOrder.forEach((id, labels) -> copy.put(id, List.copyOf(labels)));
        optionOrder = java.util.Collections.unmodifiableMap(copy);
    }

    public boolean contains(Long questionId) {
        return questionOrder.contains(questionId);
    }

    public List<String> optionsFor(Long questionId) {
        return optionOrder.getOrDefault(questionId, List.of());
    }
}
