do
$$
    begin
        if not exists(select from pg_type where typname = 'customer_type') then
            create type customer_type as enum ('INDIVIDUAL', 'PROFESSIONAL');
        end if;
    end
$$;

alter table "customer"
    add if not exists customer_type customer_type;
update "customer"
set customer_type = 'INDIVIDUAL'
where customer_type is null;