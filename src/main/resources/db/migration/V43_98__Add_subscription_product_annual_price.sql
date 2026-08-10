alter table if exists "subscription_product"
    add column if not exists annual_e2_price_id             varchar,
    add column if not exists annual_price_in_cents_with_vat bigint;
