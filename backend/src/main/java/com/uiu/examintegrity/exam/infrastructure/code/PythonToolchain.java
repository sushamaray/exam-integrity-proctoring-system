package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * Python, which has no compile step -- so this makes one.
 *
 * <p>{@code py_compile} turns a syntax error into the same thing a C student gets: a refusal
 * before anything runs, carrying the interpreter's own diagnostics. Without it a mistyped program
 * would "start" and then immediately die with a traceback, which reads as a crash rather than as
 * the typo it is.
 */
@Service
class PythonToolchain extends ProbedToolchain {
    /** Mirrors the unbuffer header the compiled languages get, for exactly the same reason. */
    private static final String UNBUFFERED_FLAG = "-u";

    private final CodeExecutionProperties limits;

    PythonToolchain(CodeExecutionProperties limits) {
        this.limits = limits;
    }

    @Override
    public Language language() {
        return Language.PYTHON;
    }

    @Override
    List<String> versionCommand() {
        return List.of(limits.getPythonPath(), "--version");
    }

    @Override
    public Toolchain.Prepared prepare(Path workspace, String source, Buffering buffering)
            throws IOException, InterruptedException {
        String fileName = language().sourceFileName();
        Files.writeString(workspace.resolve(fileName), source, StandardCharsets.UTF_8);

        Path output = workspace.resolve("compile.log");
        Process process = new ProcessBuilder(limits.getPythonPath(), "-m", "py_compile", fileName)
                .directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        process.getOutputStream().close();

        if (!process.waitFor(limits.getCompileTimeoutMs(), TimeUnit.MILLISECONDS)) {
            CodeIo.killTree(process);
            return Toolchain.Prepared.failed("Checking the program took too long and was stopped.");
        }

        String diagnostics = CodeIo.scrub(CodeIo.read(output, limits.getMaxOutputBytes()), workspace);
        if (process.exitValue() != 0) {
            return Toolchain.Prepared.failed(diagnostics.isBlank()
                    ? "Python would not accept that program, and said nothing useful."
                    : diagnostics);
        }

        // The script is named relatively on purpose. Both executors run it with the workspace as
        // the working directory, and a traceback prints the path it was given -- so a relative one
        // keeps `File "main.py", line 4` out of the business of naming a temp directory on the
        // server. The batch executor scrubs stderr anyway; the interactive console does not,
        // because it streams output as it arrives.
        return Toolchain.Prepared.runnable(diagnostics,
                List.of(limits.getPythonPath(), UNBUFFERED_FLAG, fileName));
    }
}
