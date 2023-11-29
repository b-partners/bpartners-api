do
$$
    begin
        if not exists(select from pg_type where typname = 'transaction_enable_status') then
            create type transaction_enable_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

alter table "transaction"
    add column if not exists transaction_enable_status transaction_enable_status;

update "transaction"
set transaction_enable_status = 'ENABLED'
where transaction_enable_status is null;