do
$$
    begin
        if
            not exists(select from pg_type where typname = 'transaction_status') then
            create type "transaction_status" as enum ('PENDING', 'BOOKED', 'UPCOMING', 'UNKNOWN', 'REJECTED');
        end if;
    end
$$;

alter table transaction
    add column amount integer;
alter table transaction
    add column currency varchar;
alter table transaction
    add column label varchar;
alter table transaction
    add column reference varchar;
alter table transaction
    add column side varchar;
alter table transaction
    add column status transaction_status;
alter table transaction
    add column payment_date_time timestamp;