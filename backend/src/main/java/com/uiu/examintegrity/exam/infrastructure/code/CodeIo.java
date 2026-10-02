package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bits of process and workspace handling that the batch executor and the interactive console
 * both need. Kept together so the two cannot drift on the things that matter: killing a whole
 * process tree, capping what gets read, and never showing a student a path on the server.
 */
final class CodeIo {
    private static final Logger log = LoggerFactory.getLogger(CodeIo.class);

    static final String TRUNCATION_NOTE = "\n... output truncated at the limit";

    private static final int DELETE_ATTEMPTS = 4;
    private static final long DELETE_BACKOFF_MS = 150;
    private static final long PROBE_TIMEOUT_SECONDS = 20;

    private CodeIo() {
    }

    /**
     * {@code destroyForcibly} on the process alone leaves grandchildren running, so the
     * descendants are collected first -- once the parent dies the tree is no longer reachable
     * through it.
     */
    static void killTree(Process process) {
        List<ProcessHandle> descendants = process.descendants().toList();
        process.destroyForcibly();
        descendants.forEach(handle -> handle.destroyForcibly());
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Whether a toolchain is there, asked by running something harmless like {@code --version}.
     *
     * <p>A missing executable throws rather than exiting non-zero, and Windows answers an
     * uninstalled {@code python3} with a Store advert that exits non-zero, so both the throw and
     * the exit code have to count as absent.
     */
    static boolean responds(List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            process.getOutputStream().close();
            if (!process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                killTree(process);
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException missing) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Reads at most the cap, marking the cut so a student is not left guessing. */
    static String read(Path file, int maxBytes) throws IOException {
        if (!Files.exists(file)) {
            return "";
        }
        byte[] all = Files.readAllBytes(file);
        if (all.length <= maxBytes) {
            return new String(all, StandardCharsets.UTF_8);
        }
        return new String(all, 0, maxBytes, StandardCharsets.UTF_8) + TRUNCATION_NOTE;
    }

    /** Keeps the temp paths on the server out of anything a student sees. */
    static String scrub(String text, Path workspace) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String absolute = workspace.toAbsolutePath().toString();
        return text.replace(absolute + File.separator, "").replace(absolute, "");
    }

    /**
     * Windows keeps the exe open for a moment after a kill, so a program that had to be killed
     * loses the race on the first attempt and leaves its binary behind. Retrying briefly clears
     * it; failing that, {@link #sweepStale} picks it up later.
     */
    static void deleteQuietly(Path workspace) {
        if (workspace == null) {
            return;
        }
        for (int attempt = 0; attempt < DELETE_ATTEMPTS; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(DELETE_BACKOFF_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (deleteTree(workspace)) {
                return;
            }
        }
        log.debug("Gave up deleting {}; a later sweep will take it", workspace);
    }

    static boolean deleteTree(Path root) {
        try (var walk = Files.walk(root)) {
            List<Path> deepestFirst = walk.sorted(Comparator.reverseOrder()).toList();
            boolean clean = true;
            for (Path path : deepestFirst) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException stubborn) {
                    clean = false;
                }
            }
            return clean;
        } catch (java.nio.file.NoSuchFileException gone) {
            return true;
        } catch (IOException problem) {
            return false;
        }
    }

    /**
     * Clears workspaces an earlier run could not delete, so a crash or a stubborn file handle
     * cannot let the temp directory grow without bound.
     */
    static void sweepStale(Path root, Duration staleAfter) {
        Instant cutoff = Instant.now().minus(staleAfter);
        try (var entries = Files.list(root)) {
            entries.filter(Files::isDirectory)
                    .filter(dir -> dir.getFileName().toString().startsWith("run-"))
                    .filter(dir -> olderThan(dir, cutoff))
                    .forEach(CodeIo::deleteTree);
        } catch (IOException problem) {
            log.debug("Could not sweep {}", root, problem);
        }
    }

    private static boolean olderThan(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException unknown) {
            return false;
        }
    }
}
