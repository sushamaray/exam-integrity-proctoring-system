package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.exam.domain.code.CodeExecutor;
import com.uiu.examintegrity.exam.domain.code.CodeRun;
import com.uiu.examintegrity.exam.domain.code.Language;
import com.uiu.examintegrity.exam.domain.code.RunOutcome;
import com.uiu.examintegrity.exam.domain.code.RunStatus;
import com.uiu.examintegrity.exam.domain.code.TestCase;
import com.uiu.examintegrity.exam.domain.code.TestOutcome;

/**
 * Builds and runs a submission against a fixed set of test cases, one throwaway directory per run.
 * Which compiler or interpreter that takes is {@link Toolchain}'s business, not this class's.
 *
 * <p><strong>This is hardening, not isolation.</strong> The program runs as the same OS user
 * as the server, so in principle it could read files or open sockets; the
 * {@code SourceGuard} denylist makes the obvious attempts fail loudly but is not a boundary.
 * What is genuinely enforced here is resource exhaustion: compile and run timeouts, an output
 * cap, and a bound on concurrent runs. Real isolation means another {@link CodeExecutor} --
 * a container per run, or a remote judge -- and that is the only thing that would change.
 *
 * <p>Child output goes to files rather than pipes on purpose. Reading a pipe while blocked in
 * {@code waitFor} deadlocks the moment the program fills the OS buffer, which a runaway
 * {@code printf} loop does immediately. {@link InteractiveRunner} does use pipes, and is safe
 * for the opposite reason -- see its comment.
 */
@Service
public class LocalProcessExecutor implements CodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(LocalProcessExecutor.class);

    private static final Duration STALE_AFTER = Duration.ofMinutes(10);

    private final CodeExecutionProperties limits;
    private final Toolchains toolchains;
    private final Semaphore slots;

    public LocalProcessExecutor(CodeExecutionProperties limits, Toolchains toolchains) {
        this.limits = limits;
        this.toolchains = toolchains;
        this.slots = new Semaphore(Math.max(1, limits.getMaxConcurrentRuns()));
    }

    @Override
    public boolean supports(Language language) {
        return toolchains.ready(language);
    }

    @Override
    public RunOutcome run(CodeRun run) {
        boolean acquired;
        try {
            acquired = slots.tryAcquire(limits.getQueueTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return RunOutcome.internalError("The run was interrupted.");
        }
        if (!acquired) {
            throw new ConflictException("The server is busy running other submissions; try again in a moment");
        }
        try {
            return runInWorkspace(run);
        } finally {
            slots.release();
        }
    }

    private RunOutcome runInWorkspace(CodeRun run) {
        Path workspace = null;
        try {
            Path root = Path.of(limits.getWorkDir());
            Files.createDirectories(root);
            CodeIo.sweepStale(root, STALE_AFTER);
            workspace = Files.createTempDirectory(root, "run-");

            Toolchain toolchain = toolchains.forLanguage(run.language())
                    .orElseThrow(() -> new IllegalStateException(
                            "No toolchain for " + run.language() + "; supports() should have refused this"));

            Toolchain.Prepared prepared =
                    toolchain.prepare(workspace, run.sourceCode(), Toolchain.Buffering.DEFAULT);
            if (!prepared.ok()) {
                return RunOutcome.compileError(prepared.diagnostics().isBlank()
                        ? "The compiler produced no executable and said nothing useful."
                        : prepared.diagnostics());
            }

            List<TestOutcome> outcomes = new ArrayList<>();
            for (int i = 0; i < run.tests().size(); i++) {
                outcomes.add(runTest(workspace, prepared.command(), run.tests().get(i), i));
            }
            return RunOutcome.of(outcomes, prepared.diagnostics());
        } catch (IOException problem) {
            log.warn("Local code run failed", problem);
            return RunOutcome.internalError("The code could not be run on the server.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return RunOutcome.internalError("The run was interrupted.");
        } finally {
            CodeIo.deleteQuietly(workspace);
        }
    }

    private TestOutcome runTest(Path workspace, List<String> command, TestCase test, int index)
            throws IOException, InterruptedException {
        Path stdin = workspace.resolve("in-" + index + ".txt");
        Path stdout = workspace.resolve("out-" + index + ".txt");
        Path stderr = workspace.resolve("err-" + index + ".txt");
        Files.writeString(stdin, test.input(), StandardCharsets.UTF_8);

        long startedAt = System.nanoTime();
        Process process = new ProcessBuilder(command)
                .directory(workspace.toFile())
                .redirectInput(stdin.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start();

        boolean finished = process.waitFor(limits.getRunTimeoutMs(), TimeUnit.MILLISECONDS);
        if (!finished) {
            CodeIo.killTree(process);
        }
        int elapsedMs = (int) ((System.nanoTime() - startedAt) / 1_000_000);

        boolean flooded = sizeOf(stdout) > limits.getMaxOutputBytes();
        String out = CodeIo.read(stdout, limits.getMaxOutputBytes());
        String err = CodeIo.scrub(CodeIo.read(stderr, limits.getMaxOutputBytes()), workspace);

        RunStatus status;
        if (!finished) {
            status = RunStatus.TIMEOUT;
        } else if (flooded) {
            status = RunStatus.OUTPUT_LIMIT;
        } else if (process.exitValue() != 0) {
            status = RunStatus.RUNTIME_ERROR;
        } else {
            status = RunStatus.OK;
        }
        return TestOutcome.of(test, status, out, err, elapsedMs);
    }

    private long sizeOf(Path file) throws IOException {
        return Files.exists(file) ? Files.size(file) : 0;
    }
}
