-- Reset demo accounts after load-test balance inflation (ON CONFLICT in data.sql does not overwrite).
UPDATE accounts SET balance = 10000, currency = 'INR' WHERE user_id = 1001;
UPDATE accounts SET balance = 5000, currency = 'INR' WHERE user_id = 2002;
