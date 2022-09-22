alter table "product"
    drop column id_price_reduction;
alter table "product"
    add column id_account varchar;
alter table "product"
    add column vat_percent integer;
alter table "product"
    rename column price TO unit_price;
alter table "product"
    add column created_datetime timestamp default current_timestamp;