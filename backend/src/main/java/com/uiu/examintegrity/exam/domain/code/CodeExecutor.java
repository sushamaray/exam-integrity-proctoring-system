package com.uiu.examintegrity.exam.domain.code;

/**
 * The port between the exam and whatever actually compiles and runs code.
 *
 * <p>The only implementation today is a local-process one, which is hardening rather than
 * isolation. Moving to a real sandbox -- a container per run, or a remote judge -- is a
 * matter of adding one implementation of this interface; nothing above it changes.
 */
public interface CodeExecutor {
    boolean supports(Language language);

    RunOutcome run(CodeRun run);
}
