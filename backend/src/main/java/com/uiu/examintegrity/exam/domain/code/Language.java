package com.uiu.examintegrity.exam.domain.code;

import java.util.Optional;

/**
 * The languages the selector knows about, and where each one's source file goes.
 *
 * <p>Whether a language can actually be run is deliberately <strong>not</strong> here. There are
 * two independent reasons it might not be -- nobody has written the toolchain, or this machine has
 * no compiler for it -- and a flag on the enum could only ever answer the first while appearing to
 * answer both. {@code Toolchains} answers it instead, from what is really installed.
 */
public enum Language {
    C("C (GCC)", "main.c"),
    CPP("C++ (G++)", "main.cpp"),
    PYTHON("Python 3", "main.py"),
    JAVA("Java 21", "Main.java");

    private final String label;
    private final String sourceFileName;

    Language(String label, String sourceFileName) {
        this.label = label;
        this.sourceFileName = sourceFileName;
    }

    public String label() {
        return label;
    }

    public String sourceFileName() {
        return sourceFileName;
    }

    public static Optional<Language> parse(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Language language : values()) {
            if (language.name().equalsIgnoreCase(id.trim())) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }
}
