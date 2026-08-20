alter table if exists "subscription_product"
    add column if not exists included_credits_per_billing_period bigint;

update "subscription_product"
set included_credits_per_billing_period = coalesce(free_usage_threshold, 0)
where included_credits_per_billing_period is null;
