package com.uiu.examintegrity.exam.infrastructure.code;

import java.util.List;

import com.uiu.examintegrity.support.ToolchainProbe;

/**
 * The real toolchain registry, pointed at whatever this machine actually has.
 *
 * <p>A compiler the probe cannot find is left at its default name, so {@code Toolchains.ready}
 * reports it absent and the test that needed it skips rather than failing on a path.
 */
final class TestToolchains {
    private TestToolchains() {
    }

    static Toolchains over(CodeExecutionProperties limits) {
        if (ToolchainProbe.gcc() != null) {
            limits.setGccPath(ToolchainProbe.gcc());
        }
        if (ToolchainProbe.gpp() != null) {
            limits.setGppPath(ToolchainProbe.gpp());
        }
        if (ToolchainProbe.python() != null) {
            limits.setPythonPath(ToolchainProbe.python());
        }
        GnuCompiler compiler = new GnuCompiler(limits);
        return new Toolchains(List.of(
                new CToolchain(limits, compiler),
                new CppToolchain(limits, compiler),
                new PythonToolchain(limits)));
    }
}
