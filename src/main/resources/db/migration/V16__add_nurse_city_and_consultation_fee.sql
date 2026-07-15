-- 1. Add city column to nurse_profiles table
ALTER TABLE nurse_profiles ADD COLUMN IF NOT EXISTS city VARCHAR(100);

-- 2. Add consultation_fee column to nurse_profiles table
ALTER TABLE nurse_profiles ADD COLUMN IF NOT EXISTS consultation_fee DECIMAL(10,2) DEFAULT 0.00;

-- 3. Update existing Sunita Sharma profile with a default city and fee
UPDATE nurse_profiles SET city = 'Gurugram', consultation_fee = 500.00 WHERE nurse_id = 'NUR001';

-- 4. Seed Anita Sharma (ID 12)
INSERT INTO users (id, name, mobile, role, is_active, is_mobile_verified)
VALUES (12, 'Anita Sharma', '9876543212', 'NURSE', TRUE, TRUE);

INSERT INTO nurse_profiles (
    nurse_id, user_id, gender, dob, address, qualification, experience,
    specialization, languages_spoken, latitude, longitude, registration_number,
    is_verified, is_on_duty, verification_status, average_rating, total_reviews_count,
    city, consultation_fee, created_at, updated_at
) VALUES (
    'NUR012', 12, 'Female', '1990-08-15', 'Gomti Nagar, Lucknow', 'B.Sc Nursing', '8 Years',
    'ICU Care', 'Hindi, English', 26.8467, 80.9462, 'RN123012', TRUE, TRUE, 'Approved', 4.8, 156,
    'Lucknow', 800.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO nurse_wallets (nurse_user_id, current_balance, total_earned, negative_limit, is_suspended, updated_at)
VALUES (12, 0.00, 0.00, 500.00, FALSE, CURRENT_TIMESTAMP);

-- 5. Seed Pooja Singh (ID 18)
INSERT INTO users (id, name, mobile, role, is_active, is_mobile_verified)
VALUES (18, 'Pooja Singh', '9876543218', 'NURSE', TRUE, TRUE);

INSERT INTO nurse_profiles (
    nurse_id, user_id, gender, dob, address, qualification, experience,
    specialization, languages_spoken, latitude, longitude, registration_number,
    is_verified, is_on_duty, verification_status, average_rating, total_reviews_count,
    city, consultation_fee, created_at, updated_at
) VALUES (
    'NUR018', 18, 'Female', '1995-12-05', 'Indira Nagar, Lucknow', 'GNM', '5 Years',
    'ICU Care', 'Hindi', 26.8794, 80.9813, 'RN123018', TRUE, TRUE, 'Approved', 4.6, 89,
    'Lucknow', 650.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO nurse_wallets (nurse_user_id, current_balance, total_earned, negative_limit, is_suspended, updated_at)
VALUES (18, 0.00, 0.00, 500.00, FALSE, CURRENT_TIMESTAMP);
