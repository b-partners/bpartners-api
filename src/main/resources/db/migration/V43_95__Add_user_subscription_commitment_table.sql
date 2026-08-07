do
$$
    begin
        if not exists(select from pg_type where typname = 'commitment_duration') then
            create type commitment_duration as enum ('_12_MONTHS');
        end if;
    end
$$;

create table if not exists user_subscription_commitment
(
    id                        varchar primary key default uuid_generate_v4(),
    user_id                   varchar not null,
    subscription_product_id   varchar not null,
    approval_datetime         timestamp without time zone,
    duration                  commitment_duration,
    creation_datetime         timestamp without time zone,
    commitment_start_datetime timestamp without time zone,
    commitment_end_datetime   timestamp without time zone,
    foreign key (user_id) references "user" (id),
    foreign key (subscription_product_id) references subscription_product (id)
);