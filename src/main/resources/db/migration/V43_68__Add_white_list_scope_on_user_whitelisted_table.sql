alter table "user_whitelisted"
    add column if not exists scopes jsonb;