alter table "customer"
    add column if not exists "created_at" timestamp not null default current_timestamp_utc(),
    add column if not exists "updated_at" timestamp not null default current_timestamp_utc();

