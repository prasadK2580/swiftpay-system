-- Default demo accounts only. Extra test accounts: run scripts/topup-load-test-accounts.ps1
-- (see scripts/load-test-accounts.config.json).
INSERT INTO accounts (user_id, balance, currency)
VALUES (1001, 10000, 'INR'),
       (2002, 5000, 'INR')
ON CONFLICT (user_id) DO NOTHING;