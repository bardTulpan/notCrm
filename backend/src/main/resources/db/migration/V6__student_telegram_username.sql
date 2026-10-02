-- Telegram handle for students (leads already have one; it is copied over on conversion).
ALTER TABLE students ADD COLUMN telegram_username VARCHAR(100);
