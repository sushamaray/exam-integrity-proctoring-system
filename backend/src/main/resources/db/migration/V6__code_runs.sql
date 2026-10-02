-- Wires up the code_submissions table that V3 created but nothing ever wrote to.
--
-- V3 assumed Judge0 would grade asynchronously and left judge0_token as the handle. We run
-- the compiler locally and synchronously instead, so there is nothing to poll; the column
-- stays, nullable and unused, because it is still the handle a remote judge would need.
--
-- What was missing is why a run ended. Without it a zero score is indistinguishable from a
-- program that never compiled, which is the first thing a student asks about.
ALTER TABLE code_submissions
    ADD COLUMN status         VARCHAR(24) NOT NULL DEFAULT 'OK',
    ADD COLUMN compile_output TEXT;

-- VARCHAR + CHECK rather than a native enum, consistent with every other enum column here.
-- A language we cannot run, and source the guard refuses, are rejected before anything is
-- stored, so they are deliberately not listed.
ALTER TABLE code_submissions
    ADD CONSTRAINT chk_code_status CHECK (status IN (
        'OK', 'COMPILE_ERROR', 'TIMEOUT', 'RUNTIME_ERROR', 'OUTPUT_LIMIT', 'INTERNAL_ERROR'));

-- Grading looks up the newest is_final submission per question at submit time.
CREATE INDEX idx_code_final
    ON code_submissions (session_id, question_id, is_final, created_at DESC);
