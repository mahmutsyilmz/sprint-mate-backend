-- =====================================================
-- Sprint Mate - V4 Add Email Verification Fields and waiting_for_role
-- Reason: OTP email verification flow and same-role matching feature
--         added new columns to users that were missing from the schema.
-- =====================================================

ALTER TABLE users
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN email_verification_code VARCHAR(10),
    ADD COLUMN verification_code_expires_at TIMESTAMP,
    ADD COLUMN waiting_for_role VARCHAR(50);
