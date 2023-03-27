alter table "transaction"
    add column if not exists id_bridge bigint;
alter table "transaction"
    alter column "id_swan" drop not null ;