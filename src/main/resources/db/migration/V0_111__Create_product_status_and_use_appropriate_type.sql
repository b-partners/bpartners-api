do
$$
    begin
        if not exists(select from pg_type where typname = 'product_status') then
            create type product_status as enum ('ENABLED', 'DISABLED');
        end if;
    end
$$;

alter table "invoice_product"
    alter column status drop default;
alter table "invoice_product"
    alter column status type product_status using status::product_status;
alter table "invoice_product"
    alter column status set default 'ENABLED';

alter table "product_template"
    alter column status drop default;
alter table "product_template"
    alter column status type product_status using status::product_status;
alter table "product_template"
    alter column status set default 'ENABLED';