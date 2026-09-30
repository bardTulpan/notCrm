-- Manual card position within a pipeline stage (Kanban drag reordering).
-- Zone membership (red/overdue vs normal vs paused) is still computed live from
-- health/is_paused at read time — this column only breaks ties within a zone.

ALTER TABLE students ADD COLUMN stage_position INTEGER NOT NULL DEFAULT 0;

-- Backfill existing rows with a stable order per stage, matching today's default
-- (alphabetical) sort, so nothing visibly reshuffles on deploy.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY current_stage_id ORDER BY full_name) - 1 AS rn
    FROM students
)
UPDATE students s
SET stage_position = ranked.rn
FROM ranked
WHERE s.id = ranked.id;

CREATE INDEX idx_students_stage_position ON students (current_stage_id, stage_position);
