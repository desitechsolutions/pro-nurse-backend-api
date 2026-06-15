-- Add rating metrics cache parameters to the nurse table structure
ALTER TABLE nurse_profiles ADD COLUMN IF NOT EXISTS average_rating NUMERIC(3,2) DEFAULT 0.00;
ALTER TABLE nurse_profiles ADD COLUMN IF NOT EXISTS total_reviews_count INT DEFAULT 0;

-- Ensure clear baseline indexing for duty operational updates
CREATE INDEX IF NOT EXISTS idx_nurse_profiles_duty_status ON nurse_profiles(is_on_duty, is_verified);