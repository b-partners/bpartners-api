alter table if exists "user"
    add column if not exists api_key varchar;