package com.uiu.examintegrity.exam.infrastructure.code;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * The registry of built languages, and the single answer to "can this server run that?".
 *
 * <p>Availability used to be a flag on {@link Language}. It is derived here instead because there
 * are two independent reasons a language may not be on offer -- nobody has written the toolchain
 * (Java), or this particular machine has no compiler for it -- and a hand-maintained enum flag
 * could only ever express the first, while claiming to answer the second.
 */
@Component
public class Toolchains {
    private final Map<Language, Toolchain> byLanguage = new EnumMap<>(Language.class);

    public Toolchains(List<Toolchain> toolchains) {
        toolchains.forEach(toolchain -> byLanguage.put(toolchain.language(), toolchain));
    }

    public Optional<Toolchain> forLanguage(Language language) {
        return Optional.ofNullable(byLanguage.get(language));
    }

    /** Built, and present on this machine. */
    public boolean ready(Language language) {
        return forLanguage(language).filter(Toolchain::installed).isPresent();
    }

    /** True when the language has an implementation at all, installed or not. */
    public boolean built(Language language) {
        return byLanguage.containsKey(language);
    }

    /** Everything this server can run, in selector order. */
    public List<Language> ready() {
        return java.util.Arrays.stream(Language.values()).filter(this::ready).toList();
    }

    /**
     * Why a language cannot be picked, phrased for whoever picked it.
     *
     * <p>The two cases read differently on purpose: "not supported yet" is something only we can
     * fix, while "not installed on this server" is something an administrator can.
     */
    public String refusal(Language language) {
        List<Language> offered = ready();
        if (offered.isEmpty()) {
            return "No language can be run on this server right now; tell your instructor";
        }
        String alternatives = offered.stream().map(Language::label).collect(Collectors.joining(", "));
        return built(language)
                ? language.label() + " is not installed on this server; you can use " + alternatives
                : language.label() + " is not supported yet; you can use " + alternatives;
    }
}
