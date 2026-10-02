package com.uiu.examintegrity.exam.infrastructure.code;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The compiled half of the family: write the source out, build it, run what came out. */
abstract class GnuToolchain extends ProbedToolchain {
    private static final String BINARY = "prog.exe";

    private final GnuCompiler compiler;

    GnuToolchain(GnuCompiler compiler) {
        this.compiler = compiler;
    }

    /** The compiler to invoke, as configured -- a bare name is resolved on PATH. */
    abstract String compilerPath();

    @Override
    List<String> versionCommand() {
        return List.of(compilerPath(), "--version");
    }

    @Override
    public Toolchain.Prepared prepare(Path workspace, String source, Buffering buffering)
            throws IOException, InterruptedException {
        Path file = workspace.resolve(language().sourceFileName());
        Files.writeString(file, source, StandardCharsets.UTF_8);
        return compiler.compile(workspace, language(), file, workspace.resolve(BINARY), buffering);
    }
}
