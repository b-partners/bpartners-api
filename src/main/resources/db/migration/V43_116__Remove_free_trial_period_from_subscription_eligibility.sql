update "user_subscription_eligible"
set trial_period_days = 0
where trial_period_days is null
   or trial_period_days <> 0
    and creation_datetime > '2026-09-01'