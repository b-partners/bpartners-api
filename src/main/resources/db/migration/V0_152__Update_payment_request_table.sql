do
$$
    begin
        if not exists(select from pg_type where typname = 'payment_status') then
            create type "payment_status" as enum ('PAID','UNPAID');
        end if;
    end
$$;

alter table "payment_request" add column "payment_status" payment_status default 'UNPAID';