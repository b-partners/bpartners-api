create table if not exists "bank"
(
    id        varchar
        constraint bank_pk primary key,
    bridge_id varchar,
    "name"    varchar,
    logo_url  varchar
);

alter table "account"
    add column if not exists "id_bank" varchar;