-- The first pass (V8 release) believed a single "no photo" answer from t.me, which turned out to be unreliable:
-- re-check everyone without a photo on the next run, now that "no photo" needs several answers in a row.
UPDATE student_avatars SET next_check_at = now() WHERE image IS NULL;
