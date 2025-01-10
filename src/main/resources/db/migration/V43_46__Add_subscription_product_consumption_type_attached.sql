alter table "subscription_product"
    add column if not exists consumption_type_attached subscription_consumption_type;