do
$$
    begin
        if not exists(select from pg_type where typname = 'transaction_summary_status') then
            create type transaction_summary_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

alter table "monthly_transactions_summary"
    add column if not exists status transaction_summary_status;

update "monthly_transactions_summary"
set status = 'ENABLED'
where status is null;
