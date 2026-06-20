-- Parameters (set via psql -v): sender_id, receiver_id, topup_balance
SELECT user_id, balance, currency FROM accounts WHERE user_id IN (:sender_id, :receiver_id);

UPDATE accounts SET balance = :topup_balance WHERE user_id = :sender_id;

SELECT user_id, balance, currency FROM accounts WHERE user_id IN (:sender_id, :receiver_id);
