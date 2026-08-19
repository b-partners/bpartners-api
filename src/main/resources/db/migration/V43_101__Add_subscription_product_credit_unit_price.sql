alter table if exists "subscription_product"
    add column if not exists credit_unit_price_in_cents_without_vat bigint;

update "subscription_product"
set credit_unit_price_in_cents_without_vat = overage_unit_price_in_cents
where overage_unit_price_in_cents is not null
  and credit_unit_price_in_cents_without_vat is null;

update "subscription_product"
set credit_unit_price_in_cents_without_vat = 1000
where billing_type = 'USAGE_BASED'
  and credit_unit_price_in_cents_without_vat is null;
