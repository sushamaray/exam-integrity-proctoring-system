package com.uiu.examintegrity.exam.infrastructure.code;

import java.util.List;

/**
 * A toolchain that finds out once whether its compiler is there.
 *
 * <p>The answer is cached either way: probing starts a process, and the language list is fetched
 * on every visit to the editor. Installing a compiler on a running server therefore needs a
 * restart to show up, which is the right trade for not spawning {@code gcc --version} three times
 * per page load.
 */
abstract class ProbedToolchain implements Toolchain {
    private volatile Boolean installed;

    @Override
    public boolean installed() {
        Boolean known = installed;
        if (known == null) {
            known = CodeIo.responds(versionCommand());
            installed = known;
        }
        return known;
    }

    /** Something harmless that exits zero only if the toolchain is present. */
    abstract List<String> versionCommand();
}
