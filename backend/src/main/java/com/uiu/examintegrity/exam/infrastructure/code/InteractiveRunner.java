package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.application.view.ConsoleEvent;
import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * Keeps a student's program alive while they type at it.
 *
 * <p><strong>Why pipes are safe here, when the batch executor deliberately avoids them.</strong>
 * {@link LocalProcessExecutor} redirects to files because it blocks in {@code waitFor}, and a
 * program that fills the pipe buffer while nobody is draining it deadlocks. Here nothing ever
 * blocks on {@code waitFor}: a dedicated reader thread per stream drains continuously and pushes
 * each chunk out as it arrives. That is also the only way output can reach the browser while the
 * program is still running, which is the whole point.
 *
 * <p>Programs are built {@link Toolchain.Buffering#UNBUFFERED} so a prompt with no trailing
 * newline is actually visible -- see the notes on those headers, and on Python's {@code -u}.
 *
 * <p><strong>Reserve, then launch.</strong> Starting is split in two because the broker has no
 * replay: anything published before the browser subscribes is simply lost, and a program that
 * prints and exits quickly would do exactly that. {@link #reserve} compiles and registers the
 * owner so the subscription can be authorized; the client subscribes; {@link #launch} then starts
 * the process with somebody listening.
 *
 * <p>The registry is in memory and single-instance. That is fine for one server; it would need
 * rethinking before this ran behind more than one.
 */
@Service
public class InteractiveRunner {
    private static final Logger log = LoggerFactory.getLogger(InteractiveRunner.class);

    private static final int READ_BUFFER = 2048;

    private final CodeExecutionProperties limits;
    private final ConsoleProperties console;
    private final Toolchains toolchains;
    private final Clock clock;

    private final ConcurrentHashMap<String, InteractiveRun> runs = new ConcurrentHashMap<>();

    public InteractiveRunner(CodeExecutionProperties limits, ConsoleProperties console, Toolchains toolchains,
            Clock clock) {
        this.limits = limits;
        this.console = console;
        this.toolchains = toolchains;
        this.clock = clock;
    }

    /** A build that failed -- a compile error, or a Python syntax error. Carries the diagnostics. */
    public static class CompileFailed extends RuntimeException {
        public CompileFailed(String diagnostics) {
            super(diagnostics);
        }
    }

    /** What the WebSocket interceptor asks before letting somebody subscribe to a console topic. */
    public Optional<Long> ownerOf(String runId) {
        return Optional.ofNullable(runs.get(runId)).map(InteractiveRun::ownerUserId);
    }

    /**
     * Compiles and registers a run, without starting it. The id it returns names the topic the
     * client must subscribe to before calling {@link #launch}.
     */
    public String reserve(String scope, Long ownerUserId, Language language, String sourceCode) {
        // One live program per scope. Starting another is how somebody restarts after an edit,
        // so replace rather than refuse.
        stopForScope(scope, "replaced by a new run");

        if (runs.size() >= console.getMaxConcurrent()) {
            throw new ConflictException("Too many programs are running right now; try again in a moment");
        }

        Toolchain toolchain = toolchains.forLanguage(language)
                .orElseThrow(() -> new BadRequestException(language.label() + " cannot be run on this server"));

        Path workspace = null;
        try {
            Path root = Path.of(limits.getWorkDir());
            Files.createDirectories(root);
            workspace = Files.createTempDirectory(root, "run-");

            Toolchain.Prepared prepared =
                    toolchain.prepare(workspace, sourceCode, Toolchain.Buffering.UNBUFFERED);
            if (!prepared.ok()) {
                CodeIo.deleteQuietly(workspace);
                throw new CompileFailed(prepared.diagnostics().isBlank()
                        ? "The compiler produced no executable and said nothing useful."
                        : prepared.diagnostics());
            }

            String runId = UUID.randomUUID().toString();
            runs.put(runId, new InteractiveRun(runId, ownerUserId, scope,
                    prepared.command(), workspace, clock.instant()));
            return runId;
        } catch (IOException problem) {
            CodeIo.deleteQuietly(workspace);
            log.warn("Could not prepare an interactive run", problem);
            throw new ConflictException("The program could not be prepared on the server");
        } catch (InterruptedException interrupted) {
            CodeIo.deleteQuietly(workspace);
            Thread.currentThread().interrupt();
            throw new ConflictException("The program could not be prepared on the server");
        }
    }

    /** Starts the reserved program. The client should already be subscribed to its topic. */
    public void launch(String runId, Long userId, Consumer<ConsoleEvent> publish) {
        InteractiveRun run = owned(runId, userId);
        if (!run.claimLaunch()) {
            throw new ConflictException("That program is already running");
        }
        try {
            Process process = new ProcessBuilder(run.command())
                    .directory(run.workspace().toFile())
                    .start();
            BufferedWriter stdin = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            run.started(process, stdin, clock.instant());

            publish.accept(ConsoleEvent.started());
            drain(run, process.getInputStream(), ConsoleEvent::stdout, publish);
            drain(run, process.getErrorStream(), ConsoleEvent::stderr, publish);
            awaitExit(run, publish);
        } catch (IOException problem) {
            log.warn("Could not launch interactive run {}", runId, problem);
            stop(runId, "failed to launch");
            throw new ConflictException("The program could not be started on the server");
        }
    }

    public void write(String runId, Long userId, String line, Consumer<ConsoleEvent> publish) {
        InteractiveRun run = owned(runId, userId);
        if (line.length() > console.getMaxInputChars()) {
            throw new BadRequestException("That line is too long to send");
        }
        if (!run.isAlive()) {
            throw new ConflictException("That program is not running");
        }
        try {
            run.write(line);
            run.touch(clock.instant());
        } catch (IOException closed) {
            // The program exited between the liveness check and the write, or closed its stdin.
            publish.accept(ConsoleEvent.error("The program is no longer reading input."));
        }
    }

    public void stop(String runId, Long userId) {
        owned(runId, userId);
        stop(runId, "stopped by the student");
    }

    /** Ends a run, cleans up, and says why. Safe to call more than once. */
    public void stop(String runId, String reason) {
        InteractiveRun run = runs.remove(runId);
        if (run == null || !run.claimStop()) {
            return;
        }
        run.closeStdin();
        if (run.process() != null) {
            CodeIo.killTree(run.process());
        }
        CodeIo.deleteQuietly(run.workspace());
        log.debug("Console run {} ended: {}", runId, reason);
    }

    /** Everything a sitting could have running. Used when an exam closes. */
    public static String sessionScope(Long sessionId) {
        return "session:" + sessionId;
    }

    /** One person's scratchpad, outside any exam. */
    public static String practiceScope(Long userId) {
        return "practice:" + userId;
    }

    /** Used when an exam closes, and when somebody starts a replacement program. */
    public void stopForScope(String scope, String reason) {
        runs.values().stream()
                .filter(run -> scope.equals(run.scope()))
                .map(InteractiveRun::runId)
                .toList()
                .forEach(runId -> stop(runId, reason));
    }

    /**
     * Runs that have outstayed their caps. A reserved-but-never-launched run counts once it goes
     * idle, so a student who closes the tab between reserve and launch leaves nothing behind.
     */
    public List<Expired> expired() {
        var now = clock.instant();
        return runs.values().stream()
                .map(run -> {
                    if (run.olderThan(now, console.getMaxTotalMs())) {
                        return new Expired(run.runId(), "it had been running too long");
                    }
                    if (run.idleSince(now, console.getIdleTimeoutMs())) {
                        return new Expired(run.runId(), "it sat idle too long");
                    }
                    if (run.launched() && !run.isAlive()) {
                        return new Expired(run.runId(), "it had already finished");
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public record Expired(String runId, String reason) {
    }

    public int liveCount() {
        return runs.size();
    }

    /** Lets a test tear down whatever it left behind. */
    public List<String> liveRunIds() {
        return List.copyOf(runs.keySet());
    }

    private InteractiveRun owned(String runId, Long userId) {
        InteractiveRun run = runs.get(runId);
        // Reported as missing rather than forbidden, matching the HTTP endpoints, so a run id
        // cannot be probed for existence.
        if (run == null || !run.ownerUserId().equals(userId)) {
            throw new NotFoundException("No such program");
        }
        return run;
    }

    /**
     * Drains one stream on its own thread. Reads characters rather than lines, so a prompt that
     * never ends in a newline still reaches the student.
     */
    private void drain(InteractiveRun run, InputStream stream, Function<String, ConsoleEvent> toEvent,
            Consumer<ConsoleEvent> publish) {
        Thread.ofVirtual().name("console-" + run.runId()).start(() -> {
            char[] buffer = new char[READ_BUFFER];
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (count == 0) {
                        continue;
                    }
                    String chunk = new String(buffer, 0, count);
                    run.touch(clock.instant());

                    long total = run.recordOutput(chunk.getBytes(StandardCharsets.UTF_8).length);
                    if (total > console.getMaxOutputBytes()) {
                        publish.accept(ConsoleEvent.error("This program printed too much and was stopped."));
                        stop(run.runId(), "it hit the output cap");
                        return;
                    }
                    publish.accept(toEvent.apply(chunk));
                }
            } catch (IOException ended) {
                // Normal: the pipe closes when the program exits.
                log.trace("Console stream for {} closed", run.runId(), ended);
            }
        });
    }

    /** Reports the exit code and cleans up, so nobody has to poll for the end. */
    private void awaitExit(InteractiveRun run, Consumer<ConsoleEvent> publish) {
        Thread.ofVirtual().name("console-exit-" + run.runId()).start(() -> {
            try {
                int code = run.process().waitFor();
                // Let the reader threads flush the last chunk before the exit line lands.
                Thread.sleep(80);
                if (runs.containsKey(run.runId())) {
                    publish.accept(ConsoleEvent.exited(code));
                }
                stop(run.runId(), "exited with " + code);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
