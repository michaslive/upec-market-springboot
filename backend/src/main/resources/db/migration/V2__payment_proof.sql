ALTER TABLE transactions
  ADD COLUMN IF NOT EXISTS payment_proof_url VARCHAR(1000);
