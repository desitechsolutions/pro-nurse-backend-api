-- 1. Create the Payout Request table for withdrawal tracking
CREATE TABLE payout_requests (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 wallet_id BIGINT NOT NULL,
                                 amount DECIMAL(19, 2) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 created_at DATETIME NOT NULL,
                                 processed_at DATETIME,
                                 FOREIGN KEY (wallet_id) REFERENCES nurse_wallets(id)
);

-- 2. Add support columns for Debt Enforcement to existing nurse_wallets table
ALTER TABLE nurse_wallets
    ADD COLUMN negative_limit DECIMAL(19, 2) DEFAULT 500.00 NOT NULL,
ADD COLUMN is_suspended BOOLEAN DEFAULT FALSE NOT NULL;

-- 3. Optimization: Add Index for wallet lookup by mobile (already linked to User)
-- and for status filtering on payout requests
CREATE INDEX idx_wallet_nurse_id ON nurse_wallets(nurse_user_id);
CREATE INDEX idx_payout_status ON payout_requests(status);

-- 4. Audit Log Table Indexing (Performance boost for dashboard loading)
CREATE INDEX idx_tx_wallet_id ON wallet_transactions(wallet_id);