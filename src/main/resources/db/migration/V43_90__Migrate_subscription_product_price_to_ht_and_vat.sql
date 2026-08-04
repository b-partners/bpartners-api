alter table if exists "subscription_product"
    add column if not exists vat_percent                bigint,
    add column if not exists price_in_cents_without_vat bigint;

update "subscription_product"
set vat_percent = 2000
where vat_percent is null;

update "subscription_product"
set price_in_cents_without_vat =
        (price_in_cents * 10000 + (10000 + vat_percent) / 2) / (10000 + vat_percent)
where price_in_cents is not null;

alter table if exists "subscription_product"
    drop column if exists price_in_cents;
