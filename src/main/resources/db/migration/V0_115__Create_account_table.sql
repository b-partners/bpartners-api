do
$$
    begin
        if not exists(select from pg_type where typname = 'account_status') then
            create type "account_status" as enum ('OPENED','CLOSED','CLOSING', 'SUSPENDED', 'UNKNOWN');
        end if;
    end
$$;

create table if not exists "account"
(
    id                varchar
        constraint account_pk primary key,
    id_user           varchar,
    "name"            varchar,
    iban              varchar,
    bic               varchar,
    available_balance varchar,
    status            account_status,
    constraint fk_account_user foreign key (id_user) references "user" (id)
)
