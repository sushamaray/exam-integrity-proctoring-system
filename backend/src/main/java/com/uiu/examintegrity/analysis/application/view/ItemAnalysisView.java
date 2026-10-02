package com.uiu.examintegrity.analysis.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * How every question on one exam behaved.
 *
 * @param computedAt when the statistics were last recomputed, or null if they never have been
 * @param sittings   how many closed sittings they were drawn from, so faculty can judge whether
 *                   the numbers are worth anything yet
 */
public record ItemAnalysisView(
        Long examId,
        String title,
        Instant computedAt,
        int sittings,
        List<Item> items) {

    /**
     * @param difficulty     mean fraction of the marks available that students earned. Higher is
     *                       easier. For an MCQ this is exactly the fraction who got it right.
     * @param discrimination how much better the strongest did than the weakest on this one
     *                       question. Negative means the weaker group did better, which usually
     *                       means a miskeyed or ambiguous item. Null when neither group saw it.
     * @param options        the spread across choices, for MCQ only; empty for anything else
     */
    public record Item(
            Long questionId,
            int position,
            String type,
            String text,
            int attempts,
            BigDecimal difficulty,
            BigDecimal discrimination,
            BigDecimal avgSeconds,
            List<OptionShare> options) {
    }

    /** @param correct marks the right answer, so the screen can highlight it rather than guess */
    public record OptionShare(String label, BigDecimal share, boolean correct) {
    }
}
