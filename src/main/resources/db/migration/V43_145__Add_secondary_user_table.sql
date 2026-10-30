do
$$
    begin
        if not exists(select from pg_type where typname = 'secondary_user_status') then
            create type secondary_user_status as enum ('INVITED', 'ACTIVE', 'REMOVED');
        end if;
    end
$$;

alter table if exists subscription_product
    add column if not exists max_secondary_users integer;

alter table if exists credit_transaction
    add column if not exists actor_user_id varchar;

create table if not exists secondary_user
(
    id                  varchar primary key                  default uuid_generate_v4(),
    primary_user_id     varchar                     not null,
    secondary_user_id   varchar,
    invited_email       varchar                     not null,
    status              secondary_user_status       not null default 'INVITED',
    creation_datetime   timestamp without time zone          default current_timestamp,
    update_datetime     timestamp without time zone          default current_timestamp,
    foreign key (primary_user_id) references "user" (id),
    foreign key (secondary_user_id) references "user" (id)
);

create index if not exists secondary_user_primary_user_id_idx on secondary_user (primary_user_id);

create unique index if not exists secondary_user_active_secondary_user_id_uidx
    on secondary_user (secondary_user_id) where status <> 'REMOVED';

create unique index if not exists secondary_user_active_invited_email_uidx
    on secondary_user (primary_user_id, lower(invited_email)) where status <> 'REMOVED';
