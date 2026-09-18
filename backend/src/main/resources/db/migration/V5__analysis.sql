-- Update 2: post-exam analysis -- collusion detection and per-question
-- psychometrics. Both are computed after an exam closes, never during.

CREATE TABLE similarity_pairs (
    id            BIGSERIAL PRIMARY KEY,
    exam_id       BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    session_a_id  BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    session_b_id  BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id   BIGINT REFERENCES questions(id),   -- null = whole paper
    score         NUMERIC(5,4) NOT NULL,             -- 0..1
    method        VARCHAR(32) NOT NULL,              -- TFIDF_COSINE, NGRAM, AST
    matched_spans JSONB,                             -- for the diff view
    computed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Order the ids before inserting; this stores each pair once rather than
    -- twice.
    CHECK (session_a_id < session_b_id)
);

CREATE INDEX idx_sim_exam_score ON similarity_pairs (exam_id, score DESC);

-- The CHECK above normalises the order but does not stop the same pair being
-- inserted twice by a re-run. A plain UNIQUE would not cover the whole-paper
-- rows either, because Postgres treats NULL question_id values as distinct --
-- hence COALESCE.
CREATE UNIQUE INDEX idx_sim_pair_unique
    ON similarity_pairs (exam_id, session_a_id, session_b_id,
                         COALESCE(question_id, 0), method);

CREATE TABLE item_stats (
    id                   BIGSERIAL PRIMARY KEY,
    exam_id              BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id          BIGINT NOT NULL REFERENCES questions(id),
    attempts             INT NOT NULL,
    correct_count        INT NOT NULL,
    difficulty_index     NUMERIC(5,4),   -- p = correct / attempts
    discrimination_index NUMERIC(5,4),   -- negative = broken question
    -- Share of responses per option, e.g. {"a": 0.12, "b": 0.61, ...}. The
    -- item-analysis screen draws this as a bar per choice; without it the
    -- distractor columns have no source.
    option_distribution  JSONB,
    avg_seconds          NUMERIC(8,2),
    computed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (exam_id, question_id)
);
