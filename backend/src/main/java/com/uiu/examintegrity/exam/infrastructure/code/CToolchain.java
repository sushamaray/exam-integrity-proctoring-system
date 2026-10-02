package com.uiu.examintegrity.exam.infrastructure.code;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.code.Language;

/** C, through {@code gcc}. */
@Service
class CToolchain extends GnuToolchain {
    private final CodeExecutionProperties limits;

    CToolchain(CodeExecutionProperties limits, GnuCompiler compiler) {
        super(compiler);
        this.limits = limits;
    }

    @Override
    public Language language() {
        return Language.C;
    }

    @Override
    String compilerPath() {
        return limits.getGccPath();
    }
}
