do
$$
    begin
        if not exists(select from pg_type where typname = 'subscription_consumption_type') then
            create type subscription_consumption_type as enum ('ROOF_ANALYSIS');
        end if;
    end
$$;

do
$$
    begin
        if not exists(select from pg_type where typname = 'subscription_consumption_unit') then
            create type subscription_consumption_unit as enum ('UNIT');
        end if;
    end
$$;

create table if not exists "subscription_consumption_log"
(
    id                varchar primary key,
    user_id           varchar references "user" (id),
    usage_metric      numeric,
    consumption_type  subscription_consumption_type,
    consumption_unit  subscription_consumption_unit,
    creation_datetime timestamp without time zone default current_timestamp
);