do
$$
    begin
        if not exists(select from pg_type where typname = 'payment_method_type') then
            create type payment_method_type as enum ('UNKNOWN', 'CASH', 'BANK_TRANSFER', 'CHEQUE');
        end if;
    end
$$;

alter table "invoice"
    add column payment_method payment_method_type default 'UNKNOWN';

update "invoice"
set payment_method = 'UNKNOWN'
where payment_method is null;