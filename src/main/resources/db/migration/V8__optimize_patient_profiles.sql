-- 1. Ensure string column bounds are safely explicit to prevent text overflow exploits
-- PostgreSQL uses ALTER COLUMN instead of MODIFY COLUMN
ALTER TABLE patient_profiles ALTER COLUMN patient_id TYPE VARCHAR(50);
ALTER TABLE patient_profiles ALTER COLUMN patient_id SET NOT NULL;
ALTER TABLE patient_profiles ALTER COLUMN gender TYPE VARCHAR(20);
ALTER TABLE patient_profiles ALTER COLUMN blood_group TYPE VARCHAR(10);

-- 2. Add performance index flags to handle geospatial routing
CREATE INDEX IF NOT EXISTS idx_patient_spatial_coords ON patient_profiles(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_patient_user_id ON patient_profiles(user_id);