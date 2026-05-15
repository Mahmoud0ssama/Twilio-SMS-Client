BEGIN;

DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('customer', 'administrator');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN username TYPE VARCHAR(50),
    ADD COLUMN IF NOT EXISTS role user_role NOT NULL DEFAULT 'customer',
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS birthday DATE,
    ADD COLUMN IF NOT EXISTS job VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS twilio_account_sid VARCHAR(34),
    ADD COLUMN IF NOT EXISTS twilio_auth_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS twilio_sender_id VARCHAR(34),
    ADD COLUMN IF NOT EXISTS msisdn_validated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE users ALTER COLUMN msisdn DROP NOT NULL;

ALTER TABLE sms_history ADD COLUMN IF NOT EXISTS from_phone VARCHAR(20);

UPDATE sms_history SET from_phone = 'unknown' WHERE from_phone IS NULL;

ALTER TABLE sms_history ALTER COLUMN from_phone SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_msisdn ON users(msisdn);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_sms_history_from_phone ON sms_history(from_phone);
CREATE INDEX IF NOT EXISTS idx_sms_history_to_phone ON sms_history(to_phone);

CREATE OR REPLACE FUNCTION set_users_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE set_users_updated_at();

COMMIT;
