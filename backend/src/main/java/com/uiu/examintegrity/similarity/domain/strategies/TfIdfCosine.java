package com.uiu.examintegrity.similarity.domain.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.similarity.domain.PairKey;
import com.uiu.examintegrity.similarity.domain.PairResult;
import com.uiu.examintegrity.similarity.domain.SessionAnswers;
import com.uiu.examintegrity.similarity.domain.SimilarityStrategy;
import com.uiu.examintegrity.similarity.domain.SpanMatcher;
import com.uiu.examintegrity.similarity.domain.TextNormalizer;

public final class TfIdfCosine implements SimilarityStrategy {
    public static final String METHOD = "TFIDF_COSINE";

    static final int MIN_TOKENS = 8;
    static final int MIN_DOCS_FOR_IDF = 5;
    static final int MAX_NGRAM = 3;

    @Override
    public String method() {
        return METHOD;
    }

    @Override
    public boolean supports(QuestionType type) {
        return type == QuestionType.SHORT_ANSWER || type == QuestionType.ESSAY;
    }

    @Override
    public double threshold() {
        return 0.78;
    }

    @Override
    public PairScorer prepare(List<SessionAnswers> cohort) {
        Map<Long, Map<Long, Map<String, Double>>> vectors = new HashMap<>();

        Map<Long, Map<Long, List<String>>> termsByQuestion = new HashMap<>();
        for (SessionAnswers sitting : cohort) {
            for (SessionAnswers.Answer answer : sitting.answers().values()) {
                if (!supports(answer.type()) || answer.normalized() == null) {
                    continue;
                }
                List<String> tokens = TextNormalizer.tokens(answer.normalized());
                if (tokens.size() >= MIN_TOKENS) {
                    termsByQuestion.computeIfAbsent(answer.questionId(), q -> new HashMap<>())
                            .put(sitting.sessionId(), ngrams(tokens));
                }
            }
        }

        termsByQuestion.forEach((questionId, docs) -> {
            Map<String, Integer> df = new HashMap<>();
            for (List<String> terms : docs.values()) {
                new HashSet<>(terms).forEach(term -> df.merge(term, 1, Integer::sum));
            }
            int n = docs.size();
            Map<Long, Map<String, Double>> byDoc = new HashMap<>();
            docs.forEach((sessionId, terms) -> byDoc.put(sessionId, vector(terms, df, n)));
            vectors.put(questionId, byDoc);
        });

        return (a, b) -> {
            List<PairResult> results = new ArrayList<>();
            vectors.forEach((questionId, byDoc) -> {
                Map<String, Double> va = byDoc.get(a.sessionId());
                Map<String, Double> vb = byDoc.get(b.sessionId());
                if (va == null || vb == null) {
                    return;
                }
                double score = cosine(va, vb);
                if (score <= 0) {
                    return;
                }
                String textA = a.answerTo(questionId).text();
                String textB = b.answerTo(questionId).text();
                var spans = SpanMatcher.sharedRuns(textA, textB);
                results.add(new PairResult(PairKey.of(a.sessionId(), b.sessionId()), questionId, METHOD,
                        score, spans, List.of(), spans.size() + " shared phrase(s) of five or more words"));
            });
            return results;
        };
    }

    static List<String> ngrams(List<String> tokens) {
        List<String> terms = new ArrayList<>();
        for (int size = 1; size <= MAX_NGRAM; size++) {
            for (int i = 0; i + size <= tokens.size(); i++) {
                terms.add(String.join(" ", tokens.subList(i, i + size)));
            }
        }
        return terms;
    }

    static Map<String, Double> vector(List<String> terms, Map<String, Integer> df, int n) {
        Map<String, Integer> tf = new HashMap<>();
        terms.forEach(term -> tf.merge(term, 1, Integer::sum));

        Map<String, Double> weights = new HashMap<>();
        double norm = 0;
        for (var entry : tf.entrySet()) {
            double idf = n < MIN_DOCS_FOR_IDF ? 1.0 : Math.log((1.0 + n) / (1.0 + df.get(entry.getKey())));
            double w = entry.getValue() * idf;
            if (w > 0) {
                weights.put(entry.getKey(), w);
                norm += w * w;
            }
        }
        double length = Math.sqrt(norm);
        if (length > 0) {
            weights.replaceAll((term, w) -> w / length);
        }
        return weights;
    }

    static double cosine(Map<String, Double> a, Map<String, Double> b) {
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0;
        for (var entry : small.entrySet()) {
            Double other = large.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        return Math.min(1, dot);
    }
}
