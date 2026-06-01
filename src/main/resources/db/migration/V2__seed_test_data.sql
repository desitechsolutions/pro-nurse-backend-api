-- Seed Initial Security Identity Credentials (IDs 1 and 2)
INSERT INTO users (id, name, mobile, role, is_active)
VALUES
(1, 'Sunita Sharma', '9876543210', 'NURSE', TRUE),
(2, 'Rambabu Prajapati', '9949584613', 'PATIENT', TRUE);

-- Seed Matching Mock Nurse Profile Records Linked to User ID 1
INSERT INTO nurse_profiles (
    nurse_id, user_id, gender, dob, address, qualification, experience,
    specialization, languages_spoken, latitude, longitude, registration_number,
    is_verified, is_on_duty, verification_status, created_at, updated_at
) VALUES (
    'NUR001',
    1,
    'Female',
    '1992-04-10',
    'Gurugram, Haryana',
    'GNM',
    '5 Years',
    'Post Surgery Care',
    'Hindi, English',
    28.4595,
    77.0266,
    'RN123456',
    TRUE,
    TRUE,
    'Approved',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Seed Matching Mock Patient Profile Records Linked to User ID 2
INSERT INTO patient_profiles (
    patient_id, user_id, gender, dob, blood_group, address, latitude, longitude, chronic_diseases, created_at, updated_at
) VALUES (
    'PAT001',
    2,
    'Male',
    '1995-05-15',
    'B+',
    'Delhi, India',
    26.4499,
    80.3319,
    'None',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);