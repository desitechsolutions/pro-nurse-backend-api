-- Create database indexes to optimize real-time GPS coordinate lookups
CREATE INDEX IF NOT EXISTS idx_nurse_profiles_latitude_longitude
    ON nurse_profiles(latitude, longitude);

-- Optimize the foreign key user relationship join performance
CREATE INDEX IF NOT EXISTS idx_nurse_profiles_user_id
    ON nurse_profiles(user_id);