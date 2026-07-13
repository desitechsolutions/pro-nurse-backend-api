-- Alter table to support Razorpay Digital Checkout Handshakes
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(100);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(255);

-- Alter table to capture localized storage links for uploaded patient prescriptions
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS prescription_file_path VARCHAR(255);

-- Create lookup performance indices for the payment checkout identifiers
CREATE INDEX IF NOT EXISTS idx_bookings_razorpay_order ON bookings(razorpay_order_id);