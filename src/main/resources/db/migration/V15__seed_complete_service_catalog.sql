-- ============================================================
-- V15: Seed Complete Medical Service Catalog
-- ============================================================
-- Author: ProNurse Backend Team
-- Purpose: Insert all 8 service categories shown in the Flutter
--          PatientDashboard + their subcategories with real pricing.
--          This ensures CartItem service IDs map to real DB IDs.
-- NOTE: Only inserts if not already present to prevent duplicates.
-- ============================================================

ALTER TABLE medical_services ALTER COLUMN id RESTART WITH 100;

-- ============================================================
-- Top-Level Service Categories (parent_id = NULL)
-- ============================================================
INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Nurse Visit', 'General nursing visit for assessment and care', 299.00, 45, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Nurse Visit' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Injection', 'IV/IM injection administration by certified nurse', 399.00, 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Injection' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'IV Drip', 'Intravenous drip setup and monitoring at home', 799.00, 120, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'IV Drip' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Dressing', 'Sterile wound dressing and bandage change', 499.00, 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Dressing' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Elder Care', 'Dedicated elder care and daily assistance', 1299.00, 240, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Elder Care' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Physiotherapy', 'Home physiotherapy and rehabilitation', 999.00, 60, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Physiotherapy' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'BP/Sugar Check', 'Blood pressure and blood sugar level monitoring', 149.00, 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'BP/Sugar Check' AND parent_id IS NULL);

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT NULL, 'Lab Test', 'Home blood/urine sample collection for lab tests', 599.00, 30, TRUE
WHERE NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Lab Test' AND parent_id IS NULL);

-- ============================================================
-- Sub-Services under "Nurse Visit"
-- ============================================================
INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Basic Assessment', 'Vitals check, pulse, temperature, BP', 199.00, 20, TRUE
FROM medical_services WHERE name = 'Nurse Visit' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Basic Assessment');

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Post-Surgery Care', 'Post-surgical wound care and monitoring', 799.00, 60, TRUE
FROM medical_services WHERE name = 'Nurse Visit' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Post-Surgery Care');

-- ============================================================
-- Sub-Services under "Injection"
-- ============================================================
INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'IM Injection', 'Intramuscular injection administration', 299.00, 15, TRUE
FROM medical_services WHERE name = 'Injection' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'IM Injection');

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'IV Injection', 'Intravenous injection administration', 499.00, 20, TRUE
FROM medical_services WHERE name = 'Injection' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'IV Injection');

-- ============================================================
-- Sub-Services under "Physiotherapy"
-- ============================================================
INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Stroke Rehabilitation', 'Stroke recovery and motor function rehabilitation', 1499.00, 90, TRUE
FROM medical_services WHERE name = 'Physiotherapy' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Stroke Rehabilitation');

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Orthopedic Care', 'Bone and joint physiotherapy exercises', 1199.00, 60, TRUE
FROM medical_services WHERE name = 'Physiotherapy' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Orthopedic Care');

-- ============================================================
-- Sub-Services under "Elder Care"
-- ============================================================
INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Daily Care Assistance', 'Bathing, feeding, and daily routines support', 999.00, 120, TRUE
FROM medical_services WHERE name = 'Elder Care' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Daily Care Assistance');

INSERT INTO medical_services (parent_id, name, description, base_price, estimated_duration_minutes, is_active)
SELECT id, 'Dementia Care', 'Specialized dementia and memory care support', 1799.00, 240, TRUE
FROM medical_services WHERE name = 'Elder Care' AND parent_id IS NULL
AND NOT EXISTS (SELECT 1 FROM medical_services WHERE name = 'Dementia Care');
