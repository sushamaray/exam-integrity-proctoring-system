package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.code.Language;

/**
 * Invokes the GNU toolchain -- {@code gcc} for C, {@code g++} for C++. Shared by the batch
 * executor and the interactive console so the two cannot compile the same source differently.
 */
@Service
public class GnuCompiler {
    /**
     * Forced into every interactive C compile.
     *
     * <p>C stdio is line-buffered on a terminal but <strong>fully</strong> buffered on a pipe, and
     * the console reads the program through a pipe. Without this, {@code printf("Enter n: ")} with
     * no trailing newline sits in the program's own buffer until it exits, so the student is asked
     * for input by a prompt they cannot see yet -- the console looks broken, and the obvious
     * conclusion is that input is not working.
     *
     * <p>A constructor function runs before {@code main}, so this takes effect without touching
     * the student's source or asking them to remember {@code fflush}.
     */
    private static final String UNBUFFER_HEADER_C = """
            #include <stdio.h>
            static void __attribute__((constructor)) exam_unbuffer(void) {
                setvbuf(stdout, NULL, _IONBF, 0);
                setvbuf(stderr, NULL, _IONBF, 0);
            }
            """;

    /**
     * The same for C++, where unbuffering {@code stdout} is not enough on its own.
     *
     * <p>{@code std::cout} normally writes through the {@code stdout} buffer, so the C header
     * above would cover it -- right up until the student writes
     * {@code ios_base::sync_with_stdio(false)}, which is the first line of half the C++ people
     * have ever copied off a competitive-programming page. After that {@code cout} has a buffer of
     * its own and {@code cout << "Enter n: "} vanishes again. {@code unitbuf} flushes after every
     * insertion either way.
     *
     * <p>Namespace-scope object rather than a constructor function, because it must run before
     * {@code main} in a way the C++ standard actually promises; arriving via {@code -include} puts
     * it first in the translation unit.
     */
    private static final String UNBUFFER_HEADER_CPP = """
            #include <cstdio>
            #include <iostream>
            namespace {
            struct ExamUnbuffer {
                ExamUnbuffer() {
                    setvbuf(stdout, NULL, _IONBF, 0);
                    setvbuf(stderr, NULL, _IONBF, 0);
                    std::cout.setf(std::ios::unitbuf);
                    std::cerr.setf(std::ios::unitbuf);
                }
            } exam_unbuffer_instance;
            }
            """;

    private static final String UNBUFFER_FILE_C = "exam_unbuffer.h";
    private static final String UNBUFFER_FILE_CPP = "exam_unbuffer.hpp";

    private final CodeExecutionProperties limits;

    public GnuCompiler(CodeExecutionProperties limits) {
        this.limits = limits;
    }

    /**
     * Compiles one source file to one binary.
     *
     * @return a prepared run whose command starts the binary, or a failure carrying the
     *         compiler's diagnostics
     */
    public Toolchain.Prepared compile(Path workspace, Language language, Path source, Path binary,
            Toolchain.Buffering buffering) throws IOException, InterruptedException {
        boolean cpp = language == Language.CPP;

        List<String> command = new ArrayList<>(List.of(
                cpp ? limits.getGppPath() : limits.getGccPath(),
                "-std=" + (cpp ? limits.getCppStandard() : limits.getCStandard()),
                "-O1", "-Wall",
                // The student's program is started by the server, whose PATH is not the shell's.
                // Linking the runtime in means a working compile cannot still fail to launch for
                // want of libgcc_s or libstdc++ -- a missing-DLL box that looks nothing like its
                // cause.
                "-static-libgcc"));
        if (cpp) {
            command.add("-static-libstdc++");
        }
        command.add("-o");
        command.add(binary.getFileName().toString());

        if (buffering == Toolchain.Buffering.UNBUFFERED) {
            String header = cpp ? UNBUFFER_FILE_CPP : UNBUFFER_FILE_C;
            Files.writeString(workspace.resolve(header),
                    cpp ? UNBUFFER_HEADER_CPP : UNBUFFER_HEADER_C, StandardCharsets.UTF_8);
            command.add("-include");
            command.add(header);
        }

        command.add(source.getFileName().toString());
        command.add("-lm");

        Path output = workspace.resolve("compile.log");
        Process process = new ProcessBuilder(command)
                .directory(workspace.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        process.getOutputStream().close();

        if (!process.waitFor(limits.getCompileTimeoutMs(), TimeUnit.MILLISECONDS)) {
            CodeIo.killTree(process);
            return Toolchain.Prepared.failed("The compiler took too long and was stopped.");
        }

        String diagnostics = CodeIo.scrub(CodeIo.read(output, limits.getMaxOutputBytes()), workspace);
        if (!Files.exists(binary)) {
            return Toolchain.Prepared.failed(diagnostics);
        }
        // Absolute, because ProcessBuilder resolves a bare command name on PATH rather than
        // against the working directory.
        return Toolchain.Prepared.runnable(diagnostics, List.of(binary.toAbsolutePath().toString()));
    }
}
