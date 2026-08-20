do
$$
    begin
        if not exists(select from pg_type where typname = 'billing_interval') then
            create type billing_interval as enum ('MONTHLY', 'YEARLY');
        end if;
    end
$$;

alter table if exists "user_subscription_product"
    add column if not exists billing_interval billing_interval;

update "user_subscription_product"
set billing_interval = 'MONTHLY'
where billing_interval is null;
