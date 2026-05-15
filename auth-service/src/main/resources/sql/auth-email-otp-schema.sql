-- User verification fields
ALTER TABLE users
    ADD COLUMN verified BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN verified_at TIMESTAMP NULL,
    ADD COLUMN failed_otp_attempts INT NOT NULL DEFAULT 0;

-- OTP table
CREATE TABLE otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    type VARCHAR(30) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    used BIT(1) NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_email_type_used (email, type, used),
    INDEX idx_otp_expiry_time (expiry_time)
);
