do
$$
    begin
        if not exists(select from pg_type where typname = 'enable_status') then
            create type enable_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

alter table "payment_request"
    add column if not exists enable_status enable_status not null default 'ENABLED';

create index if not exists payment_request_enable_status_index
    ON "payment_request" (enable_status);

update "payment_request"
set enable_status = 'ENABLED' where enable_status is null;