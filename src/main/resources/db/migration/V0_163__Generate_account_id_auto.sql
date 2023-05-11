alter table "account"
    alter column id set default uuid_generate_v4();