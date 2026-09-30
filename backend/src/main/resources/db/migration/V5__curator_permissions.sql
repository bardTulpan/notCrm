-- Per-curator permission flags, replacing the old binary ADMIN/CURATOR split for these
-- three cases (see prototype's canReassign/seeLeads/seeStats toggles). Meaningless for
-- ADMIN (always full access), but stored uniformly for every user for simplicity.
ALTER TABLE users ADD COLUMN see_leads BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN see_stats BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN can_reassign BOOLEAN NOT NULL DEFAULT FALSE;
