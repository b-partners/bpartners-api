alter table "user"
    add column if not exists access_token              varchar,
    add column if not exists token_expiration_datetime timestamp with time zone,
    add column if not exists token_creation_datetime   timestamp with time zone;