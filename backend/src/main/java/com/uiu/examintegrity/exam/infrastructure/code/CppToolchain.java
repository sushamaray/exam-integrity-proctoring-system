package com.uiu.examintegrity.exam.infrastructure.code;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.code.Language;

/** C++, through {@code g++}. */
@Service
class CppToolchain extends GnuToolchain {
    private final CodeExecutionProperties limits;

    CppToolchain(CodeExecutionProperties limits, GnuCompiler compiler) {
        super(compiler);
        this.limits = limits;
    }

    @Override
    public Language language() {
        return Language.CPP;
    }

    @Override
    String compilerPath() {
        return limits.getGppPath();
    }
}
