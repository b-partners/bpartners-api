do
$$
    begin
        if not exists (select 1 from pg_type where typname = 'promo_code_type') then
            create type promo_code_type as enum ('AD');
        end if;
    end
$$;

create table if not exists promo_code
(
    id                varchar primary key default uuid_generate_v4(),
    code              varchar not null unique,
    type              promo_code_type not null,
    label             varchar,
    deprecated        boolean not null default false,
    creation_datetime timestamp without time zone
);

alter table "user"
    add column if not exists promo_code_id varchar references promo_code (id);

create index if not exists idx_user_promo_code_id on "user" (promo_code_id);
