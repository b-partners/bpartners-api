create table if not exists "transaction"
(
    id         varchar
        constraint transaction_pk primary key default uuid_generate_v4(),
    id_swan    varchar not null,
    id_account varchar not null
);
create index if not exists transaction_swan_id on "transaction" (id_swan);