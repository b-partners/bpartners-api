alter table "invoice_product"
    add column if not exists status varchar default 'ENABLED';
alter table "product_template"
    add column if not exists status varchar default 'ENABLED';