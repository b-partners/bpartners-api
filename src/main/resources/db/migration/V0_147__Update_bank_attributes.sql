alter table "bank"
    alter column bridge_id type bigint using bridge_id::bigint;

alter table "bank"
    alter column id set default uuid_generate_v4();