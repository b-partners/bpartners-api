alter table if exists "user_subscription_upgrade"
    add column if not exists paid_period_start_date date,
    add column if not exists paid_period_end_date date,
    add column if not exists paid_amount_in_cents_with_vat bigint;
