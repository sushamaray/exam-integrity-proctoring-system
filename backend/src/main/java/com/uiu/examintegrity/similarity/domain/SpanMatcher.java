package com.uiu.examintegrity.similarity.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpanMatcher {
    public static final int MIN_WORDS = 5;

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private SpanMatcher() {
    }

    private record Word(String norm, int start, int end) {
    }

    public static List<Span> sharedRuns(String a, String b) {
        List<Word> wa = words(a);
        List<Word> wb = words(b);
        if (wa.size() < MIN_WORDS || wb.size() < MIN_WORDS) {
            return List.of();
        }

        int[][] run = new int[wa.size() + 1][wb.size() + 1];
        List<int[]> candidates = new ArrayList<>();
        for (int i = 1; i <= wa.size(); i++) {
            for (int j = 1; j <= wb.size(); j++) {
                if (wa.get(i - 1).norm().equals(wb.get(j - 1).norm())) {
                    run[i][j] = run[i - 1][j - 1] + 1;
                }
            }
        }

        for (int i = 1; i <= wa.size(); i++) {
            for (int j = 1; j <= wb.size(); j++) {
                int length = run[i][j];
                boolean extends_ = i < wa.size() && j < wb.size() && run[i + 1][j + 1] > length;
                if (length >= MIN_WORDS && !extends_) {
                    candidates.add(new int[] { i - length, j - length, length });
                }
            }
        }
        candidates.sort(Comparator.comparingInt((int[] c) -> -c[2]));

        boolean[] usedA = new boolean[wa.size()];
        boolean[] usedB = new boolean[wb.size()];
        List<Span> spans = new ArrayList<>();
        for (int[] c : candidates) {
            if (overlaps(usedA, c[0], c[2]) || overlaps(usedB, c[1], c[2])) {
                continue;
            }
            mark(usedA, c[0], c[2]);
            mark(usedB, c[1], c[2]);
            spans.add(new Span(wa.get(c[0]).start(), wa.get(c[0] + c[2] - 1).end(),
                    wb.get(c[1]).start(), wb.get(c[1] + c[2] - 1).end()));
        }
        spans.sort(Comparator.comparingInt(Span::aStart));
        return spans;
    }

    private static List<Word> words(String text) {
        List<Word> words = new ArrayList<>();
        if (text == null) {
            return words;
        }
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            words.add(new Word(m.group().toLowerCase(Locale.ROOT), m.start(), m.end()));
        }
        return words;
    }

    private static boolean overlaps(boolean[] used, int from, int length) {
        for (int i = from; i < from + length; i++) {
            if (used[i]) {
                return true;
            }
        }
        return false;
    }

    private static void mark(boolean[] used, int from, int length) {
        for (int i = from; i < from + length; i++) {
            used[i] = true;
        }
    }
}
