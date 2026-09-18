package com.uiu.examintegrity.exam.domain.draw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import com.uiu.examintegrity.exam.domain.QuestionDraw;

public final class RandomDraw implements QuestionDraw {
    @Override
    public List<PoolItem> select(List<PoolItem> pool, int count, RandomGenerator random) {
        List<PoolItem> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, QuestionDraw.asRandom(random));
        return shuffled.subList(0, count);
    }
}
