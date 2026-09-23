update "subscription_product"
set overage_unit_price_in_cents            = 1000,
    credit_unit_price_in_cents_without_vat = 1000
where billing_type = 'USAGE_BASED';

update "subscription_product"
set overage_unit_price_in_cents            = 1000,
    credit_unit_price_in_cents_without_vat = 1000
where id = '71fcdd0b-aa9d-485b-8beb-b3526e9587a6'
