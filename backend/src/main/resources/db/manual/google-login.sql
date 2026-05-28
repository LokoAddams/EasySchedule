-- Manual PostgreSQL/Supabase migration for Google login support.
-- Run this only if Hibernate ddl-auto=update does not apply the expected changes.

ALTER TABLE users
ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(30);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_id
ON users (google_id)
WHERE google_id IS NOT NULL;