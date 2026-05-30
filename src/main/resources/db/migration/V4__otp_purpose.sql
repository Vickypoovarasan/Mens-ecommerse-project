-- Distinguish registration OTP vs password-reset OTP (existing rows = registration)
ALTER TABLE otp_verifications
  ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'REGISTRATION' AFTER user_id;
