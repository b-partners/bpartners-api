do
$$
    begin
        if not exists(select from pg_type where typname = 'subscription_billing_type') then
            create type subscription_billing_type as enum ('COMMITMENT', 'USAGE_BASED');
        end if;
    end
$$;

alter table if exists "subscription_product"
    add column if not exists plan_code                   varchar,
    add column if not exists billing_type                subscription_billing_type,
    add column if not exists free_usage_threshold        bigint,
    add column if not exists overage_unit_price_in_cents bigint,
    add column if not exists trial_period_days           integer,
    add column if not exists annual_discount_percent     integer;

update "subscription_product"
set billing_type                = 'COMMITMENT',
    free_usage_threshold        = 20,
    overage_unit_price_in_cents = 200,
    trial_period_days           = 7
where consumption_type_attached is null;
