package com.uiu.examintegrity.exam.application.view;

import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * One entry in the language selector. Unavailable ones are shown disabled, not hidden, so the
 * roadmap stays visible.
 *
 * <p>{@code available} is what this server can really run right now, not what the enum lists --
 * see {@code Toolchains}.
 */
public record LanguageOption(String id, String label, boolean available) {
    public static LanguageOption of(Language language, boolean available) {
        return new LanguageOption(language.name(), language.label(), available);
    }
}
