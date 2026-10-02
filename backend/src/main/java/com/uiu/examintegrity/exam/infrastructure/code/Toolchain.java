package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * Everything language-specific about getting source to run: where the source file goes, what
 * counts as compiling it, and the command that then starts it.
 *
 * <p>This is the only seam the three languages differ at. Both {@link LocalProcessExecutor}
 * (batch, against test cases) and {@link InteractiveRunner} (one program a student is typing at)
 * go through it, so the two cannot build the same source differently -- a property the old
 * C-only compiler had for free by being one shared bean, and worth keeping now there are three.
 *
 * <p>Note this is <em>not</em> {@code CodeExecutor}. That port is about where code runs, and
 * swapping it for a container or a remote judge is what would buy real isolation. This is about
 * how one language is built, below that line.
 */
public interface Toolchain {
    Language language();

    /**
     * Whether the compiler or interpreter is actually on this server. Drives the language
     * selector, so an exam never offers something that would fail at the first Run.
     */
    boolean installed();

    Prepared prepare(Path workspace, String source, Buffering buffering)
            throws IOException, InterruptedException;

    /** Whether the program should write straight through instead of buffering. */
    enum Buffering {
        DEFAULT,
        UNBUFFERED,
    }

    /**
     * @param ok          whether there is something to run
     * @param diagnostics the compiler's own output, already scrubbed of server paths
     * @param command     what to start, empty when {@code ok} is false
     */
    record Prepared(boolean ok, String diagnostics, List<String> command) {
        public Prepared {
            command = List.copyOf(command);
        }

        public static Prepared failed(String diagnostics) {
            return new Prepared(false, diagnostics, List.of());
        }

        public static Prepared runnable(String diagnostics, List<String> command) {
            return new Prepared(true, diagnostics, command);
        }
    }
}
