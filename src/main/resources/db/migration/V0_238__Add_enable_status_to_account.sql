do
$$
    begin
        if not exists(select from pg_type where typname = 'account_enable_status') then
            create type account_enable_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

alter table "account"
    add column if not exists enable_status account_enable_status;

update "account"
set enable_status = 'ENABLED'
where enable_status is null;