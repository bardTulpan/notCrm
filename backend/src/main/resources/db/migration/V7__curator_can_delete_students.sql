-- Per-curator permission to delete (soft-delete) students. Off by default; ADMIN always may.
ALTER TABLE users ADD COLUMN can_delete_students BOOLEAN NOT NULL DEFAULT FALSE;
