alter table "prospect"
    add column if not exists rating numeric;
alter table "prospect"
    add column if not exists last_evaluation_date timestamp with time zone;
alter table "prospect"
    add column if not exists id_prospect_eval varchar; -- is NULLABLE

update "prospect" set rating = -1 where rating is null;