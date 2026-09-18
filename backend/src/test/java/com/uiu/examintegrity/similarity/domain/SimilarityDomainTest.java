package com.uiu.examintegrity.similarity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.similarity.domain.SessionAnswers.Answer;
import com.uiu.examintegrity.similarity.domain.strategies.TfIdfCosine;
import com.uiu.examintegrity.similarity.domain.strategies.WrongAnswerAgreement;

class SimilarityDomainTest {
    private static final long Q_TEXT = 100L;

    private static SessionAnswers written(long sessionId, String text) {
        return new SessionAnswers(sessionId, "S" + sessionId, Map.of(Q_TEXT,
                new Answer(Q_TEXT, QuestionType.SHORT_ANSWER, null, null, text, TextNormalizer.normalize(text))));
    }

    private static SessionAnswers mcq(long sessionId, String... choices) {
        Map<Long, Answer> answers = new HashMap<>();
        for (int q = 0; q < choices.length; q++) {
            answers.put((long) q, new Answer((long) q, QuestionType.MCQ, choices[q], choices[q].equals("a"), null, null));
        }
        return new SessionAnswers(sessionId, "S" + sessionId, answers);
    }

    private static double tfidf(List<SessionAnswers> cohort, SessionAnswers a, SessionAnswers b) {
        var results = new TfIdfCosine().prepare(cohort).score(a, b);
        return results.isEmpty() ? 0 : results.get(0).score();
    }

    @Test
    @DisplayName("TF-IDF: a rare shared phrase scores high, while sharing what everybody wrote scores nothing")
    void idfDoesTheWeighting() {
        String common = "the observer pattern lets a subject notify its listeners when state changes ";
        List<SessionAnswers> cohort = new ArrayList<>();
        cohort.add(written(1, common + "and a lighthouse keeper waving a red lantern at passing trawlers"));
        cohort.add(written(2, common + "and a lighthouse keeper waving a red lantern at passing trawlers"));
        cohort.add(written(3, common + "which keeps the subject decoupled from concrete listener classes"));
        cohort.add(written(4, common + "so new kinds of listener can be added without touching the subject"));
        cohort.add(written(5, common + "because the dependency points from listener towards an interface"));

        double copied = tfidf(cohort, cohort.get(0), cohort.get(1));
        double honest = tfidf(cohort, cohort.get(2), cohort.get(3));

        assertThat(copied).isGreaterThan(0.95);

        assertThat(honest).isLessThan(0.2);
    }

    @Test
    @DisplayName("TF-IDF: with too few answers to judge rarity, identical answers still match")
    void tinyCohort() {
        String text = "composition keeps each part replaceable while inheritance couples the child to its parent";
        List<SessionAnswers> cohort = List.of(written(1, text), written(2, text));
        assertThat(tfidf(cohort, cohort.get(0), cohort.get(1))).isGreaterThan(0.99);
    }

    @Test
    @DisplayName("TF-IDF: short answers are not compared, since they converge by design")
    void shortAnswersSkipped() {
        List<SessionAnswers> cohort = List.of(written(1, "Observer pattern."), written(2, "Observer pattern."),
                written(3, "It is the Observer pattern really."));
        assertThat(new TfIdfCosine().prepare(cohort).score(cohort.get(0), cohort.get(1))).isEmpty();
    }

    @Test
    @DisplayName("normalising ignores case, punctuation and spacing")
    void normalisation() {
        assertThat(TextNormalizer.normalize("  Hello,   WORLD!  It's-fine. ")).isEqualTo("hello world it s fine");
    }

    @Test
    @DisplayName("shared runs of five or more words are found, as offsets into the original text")
    void spans() {
        String a = "First, each handler can decide to handle the request. Then it ends.";
        String b = "We know each handler can decide to handle the request or pass it on.";
        List<Span> spans = SpanMatcher.sharedRuns(a, b);

        assertThat(spans).hasSize(1);
        Span span = spans.get(0);
        assertThat(a.substring(span.aStart(), span.aEnd())).isEqualTo("each handler can decide to handle the request");
        assertThat(b.substring(span.bStart(), span.bEnd())).isEqualTo("each handler can decide to handle the request");
    }

    @Test
    @DisplayName("four shared words are not a span")
    void shortRunsIgnored() {
        assertThat(SpanMatcher.sharedRuns("alpha beta gamma delta one two three", "alpha beta gamma delta four five six"))
                .isEmpty();
    }

    @Test
    @DisplayName("the plan's ratio: shared wrong options over questions both got wrong")
    void agreementRatio() {
        SessionAnswers a = mcq(1, "b", "c", "d", "a", "b");
        SessionAnswers b = mcq(2, "b", "c", "b", "c", "a");

        assertThat(WrongAnswerAgreement.agreementRatio(a, b)).isCloseTo(2.0 / 3, within(1e-9));
    }

    @Test
    @DisplayName("identical wrong answers across a paper score near certainty; a few coincidences do not")
    void wrongAnswerSignificance() {
        List<SessionAnswers> cohort = new ArrayList<>();
        cohort.add(mcq(1, "b", "c", "d", "b", "c", "d", "a", "a"));
        cohort.add(mcq(2, "b", "c", "d", "b", "c", "d", "a", "a"));
        cohort.add(mcq(3, "c", "d", "b", "b", "d", "b", "a", "a"));
        cohort.add(mcq(4, "d", "b", "c", "c", "b", "c", "a", "a"));
        cohort.add(mcq(5, "b", "d", "c", "d", "d", "c", "a", "a"));
        var scorer = new WrongAnswerAgreement().prepare(cohort);

        double copied = scorer.score(cohort.get(0), cohort.get(1)).get(0).score();
        double chance = scorer.score(cohort.get(2), cohort.get(4)).get(0).score();

        assertThat(copied).isGreaterThan(new WrongAnswerAgreement().threshold());
        assertThat(chance).isLessThan(new WrongAnswerAgreement().threshold());
        assertThat(scorer.score(cohort.get(0), cohort.get(1)).get(0).matchedQuestionIds()).hasSize(6);
    }

    @Test
    @DisplayName("agreeing on a distractor everybody fell for counts for less than agreeing on an obscure one")
    void popularDistractorsCountLess() {
        List<SessionAnswers> trap = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            trap.add(mcq(i, "b", "b", "b", "b"));
        }
        double onTrap = new WrongAnswerAgreement().prepare(trap).score(trap.get(0), trap.get(1)).get(0).score();

        List<SessionAnswers> spread = new ArrayList<>();
        spread.add(mcq(1, "b", "b", "b", "b"));
        spread.add(mcq(2, "b", "b", "b", "b"));
        String[] others = { "c", "d" };
        for (int i = 3; i <= 8; i++) {
            String o = others[i % 2];
            spread.add(mcq(i, o, o, o, o));
        }
        double onObscure = new WrongAnswerAgreement().prepare(spread).score(spread.get(0), spread.get(1)).get(0).score();

        assertThat(onObscure).isGreaterThan(onTrap);
    }

    @Test
    @DisplayName("too few shared mistakes produce no result at all")
    void minimumEvidence() {
        List<SessionAnswers> cohort = List.of(mcq(1, "b", "c", "a"), mcq(2, "b", "c", "a"));
        assertThat(new WrongAnswerAgreement().prepare(cohort).score(cohort.get(0), cohort.get(1))).isEmpty();
    }

    @Test
    @DisplayName("pair keys are stored low id first, and a pair of one is refused")
    void pairKeyOrdering() {
        assertThat(PairKey.of(19L, 12L)).isEqualTo(new PairKey(12L, 19L));
        assertThat(PairKey.swaps(19L, 12L)).isTrue();
        assertThatThrownBy(() -> PairKey.of(5L, 5L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PairKey(9L, 3L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("union-find groups chains into one component")
    void unionFind() {
        UnionFind sets = new UnionFind();
        sets.union(1L, 2L);
        sets.union(2L, 3L);
        sets.union(7L, 8L);
        assertThat(sets.find(1L)).isEqualTo(sets.find(3L));
        assertThat(sets.find(1L)).isNotEqualTo(sets.find(7L));
        assertThat(sets.sizeOf(3L)).isEqualTo(3);
        assertThat(sets.sizeOf(99L)).isEqualTo(1);
    }

    @Test
    @DisplayName("the pipeline runs load, normalise, compare, threshold, cluster in that order")
    void pipelineEndToEnd() {
        String copied = "Inheritance couples the child to the parent while a lighthouse keeper waves a red lantern";
        List<SessionAnswers> raw = List.of(
                rawWritten(30, copied),
                rawWritten(10, copied.toUpperCase() + "!!"),
                rawWritten(20, "Composition lets behaviour change at runtime by swapping one collaborator for another"),
                rawWritten(40, "Deep hierarchies are hard to follow and every level hides more assumptions from readers"));

        StrategyPipeline pipeline = new StrategyPipeline(List.of(new TfIdfCosine(), new WrongAnswerAgreement())) {
            @Override
            protected List<SessionAnswers> load(Long examId) {
                return raw;
            }
        };
        SimilarityReport report = pipeline.run(7L);

        assertThat(report.edges()).hasSize(1);
        assertThat(report.edges().get(0).pair()).isEqualTo(new PairKey(10L, 30L));
        assertThat(report.nodes()).filteredOn(n -> n.cluster() != null)
                .extracting(SimilarityReport.Node::sessionId).containsExactlyInAnyOrder(10L, 30L);
        assertThat(report.nodes()).filteredOn(n -> n.sessionId() == 20L).first()
                .satisfies(n -> assertThat(n.cluster()).isNull());
    }

    private static SessionAnswers rawWritten(long sessionId, String text) {
        return new SessionAnswers(sessionId, "S" + sessionId, Map.of(Q_TEXT,
                new Answer(Q_TEXT, QuestionType.SHORT_ANSWER, null, null, text, null)));
    }
}
