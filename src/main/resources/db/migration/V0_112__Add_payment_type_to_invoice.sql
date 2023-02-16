do
$$
    begin
        if not exists(select from pg_type where typname = 'payment_type') then
            create type payment_type as enum ('CASH', 'IN_INSTALMENT');
        end if;
    end
$$;

alter table "invoice"
    add column if not exists payment_type payment_type default 'CASH';