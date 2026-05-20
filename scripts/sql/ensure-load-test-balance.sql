-- Run before 1M load test (amount=1 per payment → need >= 1_000_000 on sender)
SELECT user_id, balance, currency FROM accounts WHERE user_id IN (1001, 2002);

UPDATE accounts SET balance = 20000000000000 WHERE user_id = 1001;

SELECT user_id, balance, currency FROM accounts WHERE user_id IN (1001, 2002);
