do
$$
begin
        if not exists(select from pg_type where typname = 'archive_status') then
create type "archive_status" as enum ('ENABLED','DISABLED');
end if;
end
$$;

alter table "invoice" add column archive_status archive_status not null default 'ENABLED';