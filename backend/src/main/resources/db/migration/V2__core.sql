-- Update 2: courses, the question bank, and exam definitions.
--
-- Schema only -- demo rows are created by DemoDataSeeder so that password
-- hashes and generated content come from the application itself.
--
-- Enums are VARCHAR + CHECK rather than native Postgres ENUM types. That keeps
-- them consistent with users.role from V1, lets Hibernate map them with a plain
-- @Enumerated(STRING), and makes adding a value a one-line CHECK change instead
-- of an ALTER TYPE (which cannot run inside Flyway's transaction).

-- ---------- people ----------

-- Faculty and invigilators have no student number, so this stays nullable.
-- Postgres treats NULLs as distinct, so the UNIQUE constraint still allows many.
ALTER TABLE users ADD COLUMN student_id VARCHAR(32) UNIQUE;

CREATE TABLE courses (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(16) NOT NULL UNIQUE,     -- 'CSE 4103'
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Carries both directions of the relationship: who sits the course and who
-- teaches it. Without role_in_course there is no way to scope the faculty
-- console to "the courses I own".
CREATE TABLE enrollments (
    id             BIGSERIAL PRIMARY KEY,
    course_id      BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    role_in_course VARCHAR(16) NOT NULL DEFAULT 'STUDENT'
                   CHECK (role_in_course IN ('STUDENT', 'INSTRUCTOR')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (course_id, user_id)
);

CREATE INDEX idx_enrollments_user ON enrollments (user_id);

-- ---------- question bank ----------

CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    course_id    BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    type         VARCHAR(16) NOT NULL
                 CHECK (type IN ('MCQ', 'SHORT_ANSWER', 'ESSAY', 'CODING')),
    topic        VARCHAR(120),
    bloom_level  VARCHAR(24),                  -- 'Remember' ... 'Create'
    difficulty   SMALLINT CHECK (difficulty BETWEEN 1 AND 5),
    body         TEXT NOT NULL,
    explanation  TEXT,                         -- shown in post-exam review
    max_marks    NUMERIC(5,2) NOT NULL DEFAULT 1,
    -- coding questions only
    starter_code TEXT,
    test_cases   JSONB,                        -- [{input, expected, hidden}]
    created_by   BIGINT REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_questions_course_type ON questions (course_id, type);

CREATE TABLE question_options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    label       VARCHAR(1) NOT NULL,           -- 'a','b','c','d' (VARCHAR, not
                                               -- CHAR: bpchar pads, and Hibernate
                                               -- maps String to varchar)
    body        TEXT NOT NULL,
    is_correct  BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (question_id, label)
);

-- ---------- exams ----------

CREATE TABLE exams (
    id                 BIGSERIAL PRIMARY KEY,
    -- No ON DELETE CASCADE: an exam and its sessions are evidence, and must not
    -- vanish because somebody tidied up a course.
    course_id          BIGINT NOT NULL REFERENCES courses(id),
    title              VARCHAR(200) NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
                       CHECK (status IN ('DRAFT', 'SCHEDULED', 'LIVE', 'CLOSED')),
    starts_at          TIMESTAMPTZ NOT NULL,
    ends_at            TIMESTAMPTZ NOT NULL,
    duration_minutes   INT NOT NULL CHECK (duration_minutes > 0),
    questions_to_draw  INT,                    -- null = use all in pool
    shuffle_questions  BOOLEAN NOT NULL DEFAULT true,
    shuffle_options    BOOLEAN NOT NULL DEFAULT true,
    allow_autocomplete BOOLEAN NOT NULL DEFAULT false,  -- coding exams
    created_by         BIGINT REFERENCES users(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at)
);

CREATE INDEX idx_exams_course_status ON exams (course_id, status);

CREATE TABLE exam_questions (
    id          BIGSERIAL PRIMARY KEY,
    exam_id     BIGINT NOT NULL REFERENCES exams(id)     ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    marks       NUMERIC(5,2),                  -- overrides question.max_marks
    UNIQUE (exam_id, question_id)
);
