alter table if exists "prospect"
    add column if not exists creation_datetime timestamp without time zone;
