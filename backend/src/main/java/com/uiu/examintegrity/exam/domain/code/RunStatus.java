package com.uiu.examintegrity.exam.domain.code;

/**
 * Why a run ended. Mirrored by the {@code chk_code_status} constraint on
 * {@code code_submissions}; a language we cannot run and source the guard refuses are
 * rejected as 400s before anything is persisted, so they are deliberately absent here.
 */
public enum RunStatus {
    OK,
    COMPILE_ERROR,
    TIMEOUT,
    RUNTIME_ERROR,
    OUTPUT_LIMIT,
    INTERNAL_ERROR;

    public boolean ran() {
        return this == OK;
    }
}
