-- Cached Telegram profile photos of students (downscaled JPEG), fetched by the backend from the public t.me page.
-- One row per student that has been checked; image is NULL when Telegram shows no photo (none, or hidden).
CREATE TABLE student_avatars (
    student_id        UUID PRIMARY KEY REFERENCES students (id) ON DELETE CASCADE,
    telegram_username VARCHAR(100) NOT NULL,
    image             BYTEA,
    image_hash        VARCHAR(16),
    checked_at        TIMESTAMPTZ NOT NULL,
    next_check_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_student_avatars_next_check ON student_avatars (next_check_at);
