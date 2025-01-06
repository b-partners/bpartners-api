alter table "user_subscription_eligible"
    add column if not exists trial_period_days integer,
    add column if not exists eligible_from     date,
    add column if not exists creation_datetime timestamp default current_timestamp;

update "user_subscription_eligible"
set trial_period_days = 0
where trial_period_days is null;

update "user_subscription_eligible"
set eligible_from = current_date - 1
where eligible_from is null;