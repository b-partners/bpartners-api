create extension if not exists "uuid-ossp";

do
$$
    begin
        if not exists(select from pg_type where typname = 'user_status') then
            create type user_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

create table if not exists "HUser"
(
    id                   varchar
        constraint user_pk primary key        default uuid_generate_v4(),
    swan_user_id         varchar     not null,
    phone_number         varchar     not null,
    monthly_subscription integer     not null,
    status               user_status not null default 'ENABLED'
);
create index if not exists swan_user_id_index on "HUser" (swan_user_id);
