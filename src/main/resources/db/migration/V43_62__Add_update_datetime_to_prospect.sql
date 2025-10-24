alter table if exists "prospect"
    add column if not exists update_datetime timestamp without time zone;
