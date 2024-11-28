do
$$
    begin
        if not exists(select from pg_type where typname = 'subscription_type') then
            create type subscription_type as enum ('MONTHLY');
        end if;
    end
$$;

alter table "subscription_product"
    alter column type type subscription_type using type::subscription_type;