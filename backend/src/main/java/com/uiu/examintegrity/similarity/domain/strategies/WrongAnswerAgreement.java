package com.uiu.examintegrity.similarity.domain.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.similarity.domain.PairKey;
import com.uiu.examintegrity.similarity.domain.PairResult;
import com.uiu.examintegrity.similarity.domain.SessionAnswers;
import com.uiu.examintegrity.similarity.domain.SimilarityStrategy;

public final class WrongAnswerAgreement implements SimilarityStrategy {
    public static final String METHOD = "WRONG_ANSWER";

    static final int MIN_BOTH_WRONG = 4;

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public boolean supports(QuestionType type) {
        return type == QuestionType.MCQ;
    }

    @Override
    public double threshold() {
        return 0.99;
    }

    public static double agreementRatio(SessionAnswers a, SessionAnswers b) {
        long bothWrong = a.questionIds().stream().filter(q -> a.isWrong(q) && b.isWrong(q)).count();
        long sharedWrong = a.questionIds().stream()
                .filter(q -> a.isWrong(q) && b.isWrong(q))
                .filter(q -> a.optionFor(q).equals(b.optionFor(q)))
                .count();
        return bothWrong == 0 ? 0 : (double) sharedWrong / bothWrong;
    }

    @Override
    public PairScorer prepare(List<SessionAnswers> cohort) {
        Map<Long, Double> chance = chanceOfMatchingWrongAnswers(cohort);
        return (a, b) -> {
            List<Long> bothWrong = a.questionIds().stream()
                    .filter(q -> a.isWrong(q) && b.isWrong(q))
                    .sorted()
                    .toList();
            if (bothWrong.size() < MIN_BOTH_WRONG) {
                return List.of();
            }
            List<Long> matched = bothWrong.stream().filter(q -> a.optionFor(q).equals(b.optionFor(q))).toList();
            double[] p = bothWrong.stream().mapToDouble(q -> chance.getOrDefault(q, 1.0 / 3)).toArray();
            double score = 1 - tailProbability(p, matched.size());

            String detail = "%d of %d shared mistakes chose the same wrong option (ratio %.2f)"
                    .formatted(matched.size(), bothWrong.size(), agreementRatio(a, b));
            return List.of(new PairResult(PairKey.of(a.sessionId(), b.sessionId()), null, METHOD, score,
                    List.of(), matched, detail));
        };
    }

    static Map<Long, Double> chanceOfMatchingWrongAnswers(List<SessionAnswers> cohort) {
        Map<Long, Map<String, Integer>> counts = new HashMap<>();
        for (SessionAnswers sitting : cohort) {
            for (Long q : sitting.questionIds()) {
                if (sitting.isWrong(q)) {
                    counts.computeIfAbsent(q, id -> new HashMap<>()).merge(sitting.optionFor(q), 1, Integer::sum);
                }
            }
        }

        Map<Long, Double> chance = new HashMap<>();
        counts.forEach((q, byOption) -> {
            int options = Math.max(3, byOption.size());
            int total = byOption.values().stream().mapToInt(Integer::intValue).sum() + options;
            double sum = 0;
            for (int count : byOption.values()) {
                double share = (count + 1.0) / total;
                sum += share * share;
            }

            int unseen = options - byOption.size();
            sum += unseen * Math.pow(1.0 / total, 2);
            chance.put(q, sum);
        });
        return chance;
    }

    static double tailProbability(double[] p, int k) {
        double[] dist = new double[p.length + 1];
        dist[0] = 1;
        for (int i = 0; i < p.length; i++) {
            for (int j = i + 1; j >= 1; j--) {
                dist[j] = dist[j] * (1 - p[i]) + dist[j - 1] * p[i];
            }
            dist[0] *= 1 - p[i];
        }
        double tail = 0;
        for (int j = k; j < dist.length; j++) {
            tail += dist[j];
        }
        return Math.min(1, tail);
    }
}
