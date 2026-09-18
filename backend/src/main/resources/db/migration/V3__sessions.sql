-- Update 2: one student's sitting of one exam, and what they produced in it.

CREATE TABLE exam_sessions (
    id             BIGSERIAL PRIMARY KEY,
    -- Deliberately no ON DELETE CASCADE from exams: see the note in V2.
    exam_id        BIGINT NOT NULL REFERENCES exams(id),
    user_id        BIGINT NOT NULL REFERENCES users(id),
    status         VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED'
                   CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED',
                                     'AUTO_SUBMITTED', 'VOIDED')),
    started_at     TIMESTAMPTZ,
    submitted_at   TIMESTAMPTZ,
    -- The deadline, resolved once at start. Auto-submit is then a single indexed
    -- query rather than recomputing started_at + duration for every open session.
    expires_at     TIMESTAMPTZ,
    -- The student's own shuffled order, so a reconnect shows the same paper.
    question_order JSONB,                      -- [12, 7, 33, ...]
    -- Option order matters for the same reason. Without it, shuffle_options
    -- re-rolls on reconnect and the paper changes under the student mid-exam.
    option_order   JSONB,                      -- {"12": ["c","a","d","b"], ...}
    consent_at     TIMESTAMPTZ,                -- briefing screen acknowledged
    -- VARCHAR rather than INET: wide enough for IPv6, and Hibernate has no clean
    -- basic type for INET, which ddl-auto=validate would reject.
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    total_marks    NUMERIC(6,2),
    grading_status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                   CHECK (grading_status IN ('PENDING', 'PARTIAL', 'COMPLETE')),
    UNIQUE (exam_id, user_id)
);

CREATE INDEX idx_sessions_exam    ON exam_sessions (exam_id, status);
-- The student dashboard asks "my exams" far more often than anything else.
CREATE INDEX idx_sessions_user    ON exam_sessions (user_id);
-- Drives the auto-submit sweep.
CREATE INDEX idx_sessions_expiry  ON exam_sessions (status, expires_at);

CREATE TABLE answers (
    id            BIGSERIAL PRIMARY KEY,
    session_id    BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id   BIGINT NOT NULL REFERENCES questions(id),
    option_id     BIGINT REFERENCES question_options(id),   -- MCQ
    text_answer   TEXT,                                     -- written
    is_flagged    BOOLEAN NOT NULL DEFAULT false,           -- student's own flag
    marks_awarded NUMERIC(5,2),
    graded_by     BIGINT REFERENCES users(id),
    graded_at     TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ,      -- when question first rendered
    answered_at   TIMESTAMPTZ,      -- last change; gap = time-on-question
    -- Stamped by @PreUpdate on the entity; a column default alone would only
    -- ever record the insert.
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (session_id, question_id)
);

CREATE INDEX idx_answers_question ON answers (question_id);

-- coding exams (populate once Judge0 is wired up)
CREATE TABLE code_submissions (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES questions(id),
    language     VARCHAR(32) NOT NULL,
    source_code  TEXT NOT NULL,
    is_final     BOOLEAN NOT NULL DEFAULT false,   -- Run vs Submit
    -- Judge0 grades asynchronously; this is the handle we poll a result with.
    judge0_token VARCHAR(64),
    passed_count INT,
    total_count  INT,
    runtime_ms   INT,
    result       JSONB,                            -- per-test detail
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_code_session ON code_submissions (session_id, created_at);
