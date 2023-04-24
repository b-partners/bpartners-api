do
$$
    begin
        if not exists(select from pg_type where typname = 'payment_status') then
            create type payment_status as enum ('PAID','UNPAID');
        end if;
    end
$$;

alter table "payment_request"
    add column "status" payment_status not null default 'UNPAID';

create index "payment_status_index" on "payment_request" ("status");