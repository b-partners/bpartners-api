alter table "calendar_event"
    add column if not exists sync boolean default false;

update "calendar_event" c
set sync = true
where c.ete_id is not null;