-- 1. Ensure string column bounds are safely explicit to prevent text overflow exploits
ALTER TABLE patient_profiles MODIFY COLUMN patient_id VARCHAR(50) NOT NULL;
ALTER TABLE patient_profiles MODIFY COLUMN gender VARCHAR(20);
ALTER TABLE patient_profiles MODIFY COLUMN blood_group VARCHAR(10);

-- 2. Add performance index flags to handle geospatial routing
CREATE INDEX IF NOT EXISTS idx_patient_spatial_coords ON patient_profiles(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_patient_user_id ON patient_profiles(user_id);