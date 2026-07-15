-- ============================================================
-- V14: Add Missing User Columns + Fix Booking Schema Gaps
-- ============================================================
-- Author: ProNurse Backend Team
-- Purpose: Align DB schema with User entity (email, passwordHash,
--          isMobileVerified) and fix booking nullability/notes gaps
-- NOTE: Never modify previous migrations. Only add here.
-- ============================================================

-- 1. Add email column (nullable — OTP users don't provide email at signup)
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(100);

-- 2. Add unique constraint on email (partial — only non-null values)
-- PostgreSQL supports partial unique indexes natively
-- For H2 compatibility we use a standard unique constraint below
-- Note: NULL values are never considered equal in SQL standard, so duplicates are safe
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- 3. Add password_hash column (nullable — OTP flow doesn't use passwords)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- 4. Add is_mobile_verified flag (already tracked in entity)
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_mobile_verified BOOLEAN DEFAULT FALSE;

-- 5. Make bookings latitude and longitude nullable
--    Mobile GPS may be unavailable; booking address is the primary location reference
ALTER TABLE bookings ALTER COLUMN latitude DROP NOT NULL;
ALTER TABLE bookings ALTER COLUMN longitude DROP NOT NULL;

-- 6. Add 'notes' field for booking-level patient instructions
--    Captures prescription notes, hasInjection flag text, and special requirements
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS notes TEXT;

-- 7. Add has_injection boolean flag for nurse visit preparation
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS has_injection BOOLEAN DEFAULT FALSE;

-- 8. Add payment_mode to bookings to persist selected payment strategy
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(30);

-- 9. Add index for faster email lookup during password-based login
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 10. Update existing OTP-verified users to reflect verification status
UPDATE users SET is_mobile_verified = TRUE WHERE is_mobile_verified IS NULL OR is_mobile_verified = FALSE;
