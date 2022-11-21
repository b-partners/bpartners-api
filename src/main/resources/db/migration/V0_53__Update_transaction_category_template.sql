do
$$
begin
        if not exists(select from pg_type where typname = 'transaction_type') then
        create type transaction_type as enum ('INCOME', 'OUTCOME');
        end if;
        end
$$;

alter table "transaction_category_template"
    add column transaction_type transaction_type;