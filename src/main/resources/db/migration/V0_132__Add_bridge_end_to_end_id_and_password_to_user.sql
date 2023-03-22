alter table "user"
    add column if not exists bridge_user_id varchar,
    add column if not exists bridge_password varchar;
