alter table if exists "subscription_product"
    add column if not exists credit_cost_per_analysis bigint;

update "subscription_product"
set credit_cost_per_analysis = 1
where credit_cost_per_analysis is null;
