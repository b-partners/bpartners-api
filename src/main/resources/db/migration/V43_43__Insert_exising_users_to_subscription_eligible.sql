INSERT INTO user_subscription_eligible (user_id)
SELECT u.id as user_id
FROM "user" u
         LEFT JOIN user_subscription_eligible e ON u.id = e.user_id
WHERE u.user_subscription_e2_id IS NULL
  AND e.user_id IS NULL;