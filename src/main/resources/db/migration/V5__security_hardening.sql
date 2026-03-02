-- V5: Security hardening — OTP hash storage
-- OTP codes are now stored as SHA-256 hashes (64 hex chars) instead of plain text.
-- Existing plain-text OTPs will be invalidated (users must re-request verification).

ALTER TABLE users ALTER COLUMN email_verification_code TYPE VARCHAR(64);
