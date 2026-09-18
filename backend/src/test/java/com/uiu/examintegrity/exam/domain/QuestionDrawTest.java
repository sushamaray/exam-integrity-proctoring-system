package com.uiu.examintegrity.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.exam.domain.QuestionDraw.DrawRules;
import com.uiu.examintegrity.exam.domain.QuestionDraw.PoolItem;
import com.uiu.examintegrity.exam.domain.draw.RandomDraw;
import com.uiu.examintegrity.exam.domain.draw.TopicBalancedDraw;

class QuestionDrawTest {
    private static final List<String> ABCD = List.of("a", "b", "c", "d");

    private static List<PoolItem> pool() {
        List<PoolItem> pool = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 6; i++) {
            pool.add(new PoolItem(id++, "Generics", ABCD));
        }
        for (int i = 0; i < 4; i++) {
            pool.add(new PoolItem(id++, "Patterns", ABCD));
        }
        for (int i = 0; i < 2; i++) {
            pool.add(new PoolItem(id++, "Concurrency", ABCD));
        }
        return pool;
    }

    @Test
    @DisplayName("the whole pool is drawn once each, and a seed reproduces the paper exactly")
    void randomDrawIsAPermutationAndReproducible() {
        DrawRules rules = new DrawRules(null, true, true);
        Paper first = new RandomDraw().draw(pool(), rules, new Random(42));
        Paper again = new RandomDraw().draw(pool(), rules, new Random(42));

        assertThat(first.questionOrder()).hasSize(12).doesNotHaveDuplicates();
        assertThat(first).isEqualTo(again);
        assertThat(first.optionsFor(1L)).containsExactlyInAnyOrderElementsOf(ABCD);
    }

    @Test
    @DisplayName("with shuffling off, the author's order and option order are kept")
    void noShuffleKeepsOrder() {
        Paper paper = new RandomDraw().draw(pool(), new DrawRules(null, false, false), new Random(7));
        assertThat(paper.questionOrder()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        assertThat(paper.optionsFor(5L)).containsExactlyElementsOf(ABCD);
    }

    @Test
    @DisplayName("a balanced subset takes from every topic before doubling up on any")
    void topicBalancedSpreadsTopics() {
        Map<Long, String> topicOf = pool().stream()
                .collect(Collectors.toMap(PoolItem::questionId, PoolItem::topic));

        for (int seed = 0; seed < 50; seed++) {
            Paper paper = new TopicBalancedDraw().draw(pool(), new DrawRules(6, true, true), new Random(seed));
            Map<String, Long> perTopic = paper.questionOrder().stream()
                    .collect(Collectors.groupingBy(topicOf::get, Collectors.counting()));

            assertThat(paper.questionOrder()).hasSize(6).doesNotHaveDuplicates();

            assertThat(perTopic).containsEntry("Generics", 2L)
                    .containsEntry("Patterns", 2L)
                    .containsEntry("Concurrency", 2L);
        }
    }

    @Test
    @DisplayName("a small topic running out does not stop the draw reaching its count")
    void topicBalancedFillsFromLargerTopics() {
        Paper paper = new TopicBalancedDraw().draw(pool(), new DrawRules(10, true, true), new Random(3));
        Map<Long, PoolItem> byId = pool().stream()
                .collect(Collectors.toMap(PoolItem::questionId, Function.identity()));
        long concurrency = paper.questionOrder().stream()
                .filter(id -> byId.get(id).topic().equals("Concurrency")).count();

        assertThat(paper.questionOrder()).hasSize(10).doesNotHaveDuplicates();
        assertThat(concurrency).isEqualTo(2);
    }

    @Test
    @DisplayName("asking for more than the pool holds draws the whole pool")
    void countCappedByPool() {
        Paper paper = new RandomDraw().draw(pool(), new DrawRules(50, true, true), new Random(1));
        assertThat(paper.questionOrder()).hasSize(12);
    }
}
