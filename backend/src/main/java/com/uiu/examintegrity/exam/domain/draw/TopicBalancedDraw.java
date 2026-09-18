package com.uiu.examintegrity.exam.domain.draw;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import com.uiu.examintegrity.exam.domain.QuestionDraw;

public final class TopicBalancedDraw implements QuestionDraw {
    private static final String NO_TOPIC = "";

    @Override
    public List<PoolItem> select(List<PoolItem> pool, int count, RandomGenerator random) {
        java.util.Random rng = QuestionDraw.asRandom(random);

        Map<String, List<PoolItem>> byTopic = new LinkedHashMap<>();
        for (PoolItem item : pool) {
            byTopic.computeIfAbsent(item.topic() == null ? NO_TOPIC : item.topic(),
                    topic -> new ArrayList<>()).add(item);
        }

        List<Deque<PoolItem>> groups = new ArrayList<>();
        for (List<PoolItem> group : byTopic.values()) {
            List<PoolItem> shuffled = new ArrayList<>(group);
            Collections.shuffle(shuffled, rng);
            groups.add(new ArrayDeque<>(shuffled));
        }

        Collections.shuffle(groups, rng);

        List<PoolItem> selected = new ArrayList<>(count);
        while (selected.size() < count) {
            for (Deque<PoolItem> group : groups) {
                if (!group.isEmpty() && selected.size() < count) {
                    selected.add(group.poll());
                }
            }
        }

        Collections.shuffle(selected, rng);
        return selected;
    }
}
