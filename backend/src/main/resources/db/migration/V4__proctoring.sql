-- Update 2: the raw event stream, the score computed from it, and the flags a
-- human has to decide on.

CREATE TABLE proctoring_events (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    -- Minted by the browser. The client retries on a dropped connection, and
    -- without this a retry would be counted twice -- inflating the one number
    -- this system has to be able to defend.
    client_event_id UUID NOT NULL,
    event_type  VARCHAR(32) NOT NULL,   -- PASTE, TAB_HIDDEN, WINDOW_BLUR,
                                        -- FS_EXIT, COPY, HEARTBEAT, DEVTOOLS
    occurred_at TIMESTAMPTZ NOT NULL,   -- client clock
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),  -- server clock
    question_id BIGINT REFERENCES questions(id),
    payload     JSONB NOT NULL DEFAULT '{}',
    UNIQUE (session_id, client_event_id)
);

CREATE INDEX idx_events_session_time ON proctoring_events (session_id, occurred_at);
CREATE INDEX idx_events_type         ON proctoring_events (event_type);
-- Makes payload->>'charCount' > '200' and friends fast.
CREATE INDEX idx_events_payload      ON proctoring_events USING GIN (payload);

-- Current risk per session, recomputed as events arrive. One row, not a
-- history: the live monitor only needs current state, and proctoring_events
-- already holds everything needed to reconstruct how we got here.
CREATE TABLE risk_scores (
    session_id        BIGINT PRIMARY KEY REFERENCES exam_sessions(id) ON DELETE CASCADE,
    score             NUMERIC(5,2) NOT NULL DEFAULT 0,
    tier              VARCHAR(16) NOT NULL DEFAULT 'CLEAR'
                      CHECK (tier IN ('CLEAR', 'REVIEW', 'ESCALATE')),
    cohort_percentile NUMERIC(5,2),    -- rank within this exam's cohort
    breakdown         JSONB NOT NULL DEFAULT '[]',
    -- [{signal, weight, confidence, eventIds:[...]}]
    -- Backs @Version. Several event handlers recompute this row concurrently,
    -- and a lost update here silently changes a student's score.
    version           INT NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE flags (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    reason       VARCHAR(64) NOT NULL,     -- CORRELATED_CLUSTER, LARGE_PASTE, ...
    status       VARCHAR(16) NOT NULL DEFAULT 'RAISED'
                 CHECK (status IN ('RAISED', 'UNDER_REVIEW', 'UPHELD', 'DISMISSED')),
    confidence   NUMERIC(4,3),
    window_start TIMESTAMPTZ,
    window_end   TIMESTAMPTZ,
    -- JSONB rather than BIGINT[]: every other evidence column here is already
    -- JSONB, and Hibernate maps List<Long> through @JdbcTypeCode(JSON) without
    -- the array-type fuss.
    event_ids    JSONB NOT NULL DEFAULT '[]',   -- events that formed the cluster
    reviewed_by  BIGINT REFERENCES users(id),
    reviewed_at  TIMESTAMPTZ,
    reviewer_note TEXT,
    student_note TEXT,                     -- student's own explanation
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_flags_status ON flags (status, created_at);

-- Re-running the risk engine over the same session must not stack up duplicate
-- copies of a flag somebody is already looking at. Resolved flags are excluded,
-- so the same reason can legitimately be raised again later.
CREATE UNIQUE INDEX idx_flags_open_unique
    ON flags (session_id, reason)
    WHERE status IN ('RAISED', 'UNDER_REVIEW');
