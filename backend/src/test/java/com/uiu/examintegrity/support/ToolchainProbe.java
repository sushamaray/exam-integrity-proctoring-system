package com.uiu.examintegrity.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Finds the compilers and interpreters the tests need.
 *
 * <p>Tests that actually build something are skipped rather than failed where there is no
 * toolchain, so CI without MinGW or Python stays green. The tests that matter for security --
 * who may call the endpoints, and what comes back in the response -- never reach a compiler and
 * are not gated.
 */
public final class ToolchainProbe {
    private static final String MINGW = "C:/MinGW/bin/";

    private ToolchainProbe() {
    }

    /** The C compiler to use, or null when there is none. */
    public static String gcc() {
        return resolve("gcc", MINGW + "gcc.exe");
    }

    /** The C++ compiler to use, or null when there is none. */
    public static String gpp() {
        return resolve("g++", MINGW + "g++.exe");
    }

    /**
     * The Python interpreter to use, or null when there is none. {@code python3} is not consulted:
     * on Windows it is an App Execution Alias for the Microsoft Store, which answers
     * {@code --version} with an advert and a non-zero exit -- hence the exit code check rather
     * than just looking for the file.
     */
    public static String python() {
        return resolve("python", null);
    }

    public static boolean hasGcc() {
        return gcc() != null;
    }

    public static boolean hasGpp() {
        return gpp() != null;
    }

    public static boolean hasPython() {
        return python() != null;
    }

    private static String resolve(String onPath, String fallback) {
        if (runs(onPath)) {
            return onPath;
        }
        if (fallback == null) {
            return null;
        }
        return Files.isExecutable(Path.of(fallback)) && runs(fallback) ? fallback : null;
    }

    private static boolean runs(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception missing) {
            return false;
        }
    }
}
