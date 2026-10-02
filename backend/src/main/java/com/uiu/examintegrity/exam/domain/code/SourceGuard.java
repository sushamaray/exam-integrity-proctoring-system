package com.uiu.examintegrity.exam.domain.code;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Refuses submissions that obviously reach outside the exam -- process spawning, the
 * filesystem, sockets, inline assembly.
 *
 * <p>This is defence in depth and explicitly <strong>not</strong> a security boundary: a
 * determined student can get around a textual denylist. The boundary belongs to whatever
 * implements {@link CodeExecutor}, and the local-process executor does not provide one.
 * The guard is here so the common cases fail with a clear message instead of quietly
 * working.
 *
 * <p>The lists are per language because the escapes are: {@code fopen} means nothing in Python,
 * and banning {@code open(} in C would refuse every second correct answer by catching
 * {@code fopen(}. What they have in common is being phrased as a reason rather than a rule, so a
 * student who trips one learns what to do instead.
 */
public final class SourceGuard {
    private SourceGuard() {
    }

    private record Banned(String needle, String why) {
    }

    /** C and C++. Lower case, because the haystack is lowered before matching. */
    private static final List<Banned> NATIVE = List.of(
            new Banned("#include <windows.h>", "the Windows API is not available in an exam"),
            new Banned("#include <winsock", "networking is not available in an exam"),
            new Banned("#include <process.h>", "starting processes is not allowed"),
            new Banned("system(", "system() is not allowed"),
            new Banned("popen(", "popen() is not allowed"),
            new Banned("fork(", "starting processes is not allowed"),
            new Banned("execl", "starting processes is not allowed"),
            new Banned("execv", "starting processes is not allowed"),
            new Banned("execp", "starting processes is not allowed"),
            new Banned("spawnl", "starting processes is not allowed"),
            new Banned("createprocess", "starting processes is not allowed"),
            new Banned("fopen(", "reading or writing files is not allowed; use stdin and stdout"),
            new Banned("freopen(", "reading or writing files is not allowed; use stdin and stdout"),
            new Banned("fstream", "reading or writing files is not allowed; use cin and cout"),
            new Banned("filesystem", "reading or writing files is not allowed; use cin and cout"),
            new Banned("remove(", "deleting files is not allowed"),
            new Banned("rename(", "renaming files is not allowed"),
            new Banned("socket(", "networking is not available in an exam"),
            new Banned("__asm", "inline assembly is not allowed"),
            new Banned("asm(", "inline assembly is not allowed"));

    /**
     * Python, where almost everything out of bounds arrives as an import.
     *
     * <p>{@code sys} is deliberately absent: {@code sys.stdin} is how you read input without
     * {@code input()}, and refusing it would refuse correct answers. {@code compile(} is absent
     * too -- {@code re.compile(} is ordinary.
     */
    private static final List<Banned> PYTHON = List.of(
            new Banned("import os", "the operating system module is not available in an exam"),
            new Banned("from os", "the operating system module is not available in an exam"),
            new Banned("import subprocess", "starting processes is not allowed"),
            new Banned("from subprocess", "starting processes is not allowed"),
            new Banned("import multiprocessing", "starting processes is not allowed"),
            new Banned("import socket", "networking is not available in an exam"),
            new Banned("import ssl", "networking is not available in an exam"),
            new Banned("urllib", "networking is not available in an exam"),
            new Banned("requests", "networking is not available in an exam"),
            new Banned("http.client", "networking is not available in an exam"),
            new Banned("import shutil", "reading or writing files is not allowed; use input() and print()"),
            new Banned("import pathlib", "reading or writing files is not allowed; use input() and print()"),
            new Banned("import glob", "reading or writing files is not allowed; use input() and print()"),
            new Banned("import tempfile", "reading or writing files is not allowed; use input() and print()"),
            new Banned("open(", "reading or writing files is not allowed; use input() and print()"),
            new Banned("import ctypes", "calling into native code is not allowed"),
            new Banned("import pickle", "loading objects from bytes is not allowed"),
            new Banned("import marshal", "loading objects from bytes is not allowed"),
            new Banned("importlib", "loading modules at run time is not allowed"),
            new Banned("__import__", "loading modules at run time is not allowed"),
            new Banned("eval(", "running code built at run time is not allowed"),
            new Banned("exec(", "running code built at run time is not allowed"));

    /** The reason to refuse this source, or empty when it is acceptable. */
    public static Optional<String> reject(Language language, String source) {
        if (source == null || source.isBlank()) {
            return Optional.of("There is no code to run");
        }
        String haystack = source.toLowerCase();
        for (Banned banned : bannedFor(language)) {
            if (haystack.contains(banned.needle())) {
                return Optional.of(banned.why());
            }
        }
        return Optional.empty();
    }

    /**
     * Anything without a list of its own gets both, rather than none. Nothing runs Java yet, so
     * this is unreachable today; if something ever does, it starts out too strict instead of
     * unguarded.
     */
    private static List<Banned> bannedFor(Language language) {
        return switch (language) {
            case C, CPP -> NATIVE;
            case PYTHON -> PYTHON;
            case JAVA -> Stream.concat(NATIVE.stream(), PYTHON.stream()).toList();
        };
    }
}
