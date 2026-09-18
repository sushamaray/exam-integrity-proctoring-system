package com.uiu.examintegrity.similarity.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {
    private static final Pattern NOT_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return NOT_WORD.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    public static List<String> tokens(String normalized) {
        return normalized.isEmpty() ? List.of() : Arrays.asList(normalized.split(" "));
    }
}
