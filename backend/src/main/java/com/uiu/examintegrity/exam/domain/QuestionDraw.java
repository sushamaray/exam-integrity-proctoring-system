package com.uiu.examintegrity.exam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

public interface QuestionDraw {
    List<PoolItem> select(List<PoolItem> pool, int count, RandomGenerator random);

    default Paper draw(List<PoolItem> pool, DrawRules rules, RandomGenerator random) {
        int count = rules.questionsToDraw() == null
                ? pool.size()
                : Math.min(rules.questionsToDraw(), pool.size());

        List<PoolItem> selected = new ArrayList<>(select(pool, count, random));
        if (!rules.shuffleQuestions()) {
            selected.sort(Comparator.comparingInt(pool::indexOf));
        }

        List<Long> order = new ArrayList<>();
        Map<Long, List<String>> optionOrder = new LinkedHashMap<>();
        for (PoolItem item : selected) {
            order.add(item.questionId());
            List<String> labels = new ArrayList<>(item.optionLabels());
            if (rules.shuffleOptions()) {
                Collections.shuffle(labels, asRandom(random));
            }
            optionOrder.put(item.questionId(), labels);
        }
        return new Paper(order, optionOrder);
    }

    static java.util.Random asRandom(RandomGenerator random) {
        return random instanceof java.util.Random r ? r : java.util.Random.from(random);
    }

    record PoolItem(Long questionId, String topic, List<String> optionLabels) {
    }

    record DrawRules(Integer questionsToDraw, boolean shuffleQuestions, boolean shuffleOptions) {
    }
}
