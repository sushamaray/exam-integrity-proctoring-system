package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One program a student is typing at, from the moment it is built to the moment it is cleaned up.
 *
 * <p>It exists before it runs: {@code reserve} compiles and registers it so the WebSocket
 * subscription can be authorized, and {@code launch} fills in the process once the client is
 * listening. So {@link #process()} is null for a short, legitimate window.
 *
 * <p>Touched from several threads -- the request thread writing input, two reader threads draining
 * output, and the sweeper deciding whether it has outstayed its welcome -- so the counters are
 * atomics and the mutable fields are volatile.
 */
class InteractiveRun {
    private final String runId;
    private final Long ownerUserId;
    private final String scope;
    private final List<String> command;
    private final Path workspace;
    private final Instant reservedAt;

    private final AtomicLong emittedBytes = new AtomicLong();
    private final AtomicBoolean launchClaimed = new AtomicBoolean();
    private final AtomicBoolean stopClaimed = new AtomicBoolean();

    private volatile Process process;
    private volatile BufferedWriter stdin;
    private volatile Instant lastActivityAt;

    InteractiveRun(String runId, Long ownerUserId, String scope,
            List<String> command, Path workspace, Instant reservedAt) {
        this.runId = runId;
        this.ownerUserId = ownerUserId;
        this.scope = scope;
        this.command = List.copyOf(command);
        this.workspace = workspace;
        this.reservedAt = reservedAt;
        this.lastActivityAt = reservedAt;
    }

    String runId() {
        return runId;
    }

    Long ownerUserId() {
        return ownerUserId;
    }

    /**
     * What this run belongs to: one sitting, or one person's scratchpad. Starting a second run in
     * the same scope replaces the first, which is how both "restart after an edit" and "a program
     * must not outlive its exam" are expressed.
     */
    String scope() {
        return scope;
    }

    /** What starts the program: a compiled binary, or an interpreter and a script. */
    List<String> command() {
        return command;
    }

    Path workspace() {
        return workspace;
    }

    /** Null until the run is launched. */
    Process process() {
        return process;
    }

    /** True the first time only, so a repeated launch cannot start a second process. */
    boolean claimLaunch() {
        return launchClaimed.compareAndSet(false, true);
    }

    boolean launched() {
        return launchClaimed.get();
    }

    void started(Process started, BufferedWriter writer, Instant now) {
        this.process = started;
        this.stdin = writer;
        this.lastActivityAt = now;
    }

    boolean isAlive() {
        Process current = process;
        return current != null && current.isAlive();
    }

    void touch(Instant now) {
        lastActivityAt = now;
    }

    /** Returns the running total, so the caller can notice it has crossed the cap. */
    long recordOutput(int bytes) {
        return emittedBytes.addAndGet(bytes);
    }

    boolean idleSince(Instant now, long idleTimeoutMs) {
        return lastActivityAt.plusMillis(idleTimeoutMs).isBefore(now);
    }

    boolean olderThan(Instant now, long maxTotalMs) {
        return reservedAt.plusMillis(maxTotalMs).isBefore(now);
    }

    /** True the first time only, so cleanup happens exactly once. */
    boolean claimStop() {
        return stopClaimed.compareAndSet(false, true);
    }

    void write(String line) throws IOException {
        BufferedWriter writer = stdin;
        if (writer == null) {
            throw new IOException("not launched");
        }
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    void closeStdin() {
        BufferedWriter writer = stdin;
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException alreadyGone) {
            // The program exited first and closed its end; nothing to do.
        }
    }
}
