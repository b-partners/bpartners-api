do
$$
begin
    if not exists(select from pg_type where typname = 'customer_status') then
        create type customer_status as enum ('ENABLED', 'DISABLED');
    end if;
end
$$;

alter table customer add column status customer_status not null default 'ENABLED';