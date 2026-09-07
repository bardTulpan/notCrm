-- Pipeline CRM initial schema

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(100) NOT NULL CONSTRAINT uq_users_username UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    avatar_color    VARCHAR(7),
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE pipeline_stages (
    id              UUID PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    position        INTEGER NOT NULL CONSTRAINT uq_pipeline_stages_position UNIQUE,
    norm_days       INTEGER,
    is_final        BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_pipeline_stages_norm_days CHECK (norm_days IS NULL OR norm_days > 0),
    CONSTRAINT chk_pipeline_stages_position CHECK (position >= 0)
);

CREATE TABLE cohorts (
    id              UUID PRIMARY KEY,
    name            VARCHAR(100) NOT NULL CONSTRAINT uq_cohorts_name UNIQUE,
    start_date      DATE NOT NULL CONSTRAINT uq_cohorts_start_date UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    archived_at     TIMESTAMPTZ
);

CREATE TABLE leads (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(200) NOT NULL,
    telegram_username       VARCHAR(100),
    price_description       VARCHAR(255),
    postpay_percent         INTEGER,
    next_ping_at            TIMESTAMPTZ,
    status                  VARCHAR(20) NOT NULL,
    assigned_curator_id     UUID REFERENCES users (id),
    created_by_id           UUID NOT NULL REFERENCES users (id),
    converted_student_id    UUID CONSTRAINT uq_leads_converted_student UNIQUE,
    archived_at             TIMESTAMPTZ,
    archived_by_id          UUID REFERENCES users (id),
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    deleted_at              TIMESTAMPTZ,
    CONSTRAINT chk_leads_postpay_percent CHECK (postpay_percent IS NULL OR (postpay_percent >= 0 AND postpay_percent <= 100))
);

CREATE INDEX idx_leads_status ON leads (status);
CREATE INDEX idx_leads_assigned_curator ON leads (assigned_curator_id);
CREATE INDEX idx_leads_deleted_at ON leads (deleted_at);

CREATE TABLE lead_notes (
    id              UUID PRIMARY KEY,
    lead_id         UUID NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    text            TEXT NOT NULL,
    position        INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_lead_notes_lead ON lead_notes (lead_id);

CREATE TABLE students (
    id                  UUID PRIMARY KEY,
    full_name           VARCHAR(200) NOT NULL,
    source_lead_id      UUID CONSTRAINT uq_students_source_lead UNIQUE,
    current_stage_id    UUID NOT NULL,
    curator_id          UUID NOT NULL REFERENCES users (id),
    cohort_id           UUID REFERENCES cohorts (id),
    stage_entered_at    TIMESTAMPTZ NOT NULL,
    started_at          TIMESTAMPTZ NOT NULL,
    is_paused           BOOLEAN NOT NULL DEFAULT FALSE,
    paused_at           TIMESTAMPTZ,
    postpay_percent     INTEGER,
    created_by_id       UUID NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT chk_students_postpay_percent CHECK (postpay_percent IS NULL OR (postpay_percent >= 0 AND postpay_percent <= 100))
);

CREATE INDEX idx_students_curator ON students (curator_id);
CREATE INDEX idx_students_current_stage ON students (current_stage_id);
CREATE INDEX idx_students_cohort ON students (cohort_id);
CREATE INDEX idx_students_deleted_at ON students (deleted_at);

CREATE TABLE student_notes (
    id              UUID PRIMARY KEY,
    student_id      UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    text            TEXT NOT NULL,
    position        INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_student_notes_student ON student_notes (student_id);

CREATE TABLE student_stage_history (
    id              UUID PRIMARY KEY,
    student_id      UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    stage_id        UUID NOT NULL REFERENCES pipeline_stages (id),
    entered_at      TIMESTAMPTZ NOT NULL,
    exited_at       TIMESTAMPTZ,
    changed_by_id   UUID NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_student_stage_history_student ON student_stage_history (student_id);
CREATE INDEX idx_student_stage_history_open ON student_stage_history (student_id, exited_at);

CREATE TABLE student_curator_history (
    id                  UUID PRIMARY KEY,
    student_id          UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    from_curator_id     UUID REFERENCES users (id),
    to_curator_id       UUID NOT NULL REFERENCES users (id),
    changed_by_id       UUID NOT NULL REFERENCES users (id),
    changed_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_student_curator_history_student ON student_curator_history (student_id);

CREATE TABLE student_comments (
    id              UUID PRIMARY KEY,
    student_id      UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users (id),
    text            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_student_comments_student ON student_comments (student_id);

CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    actor_id        UUID REFERENCES users (id),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    before_data     JSONB,
    after_data      JSONB,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id);
