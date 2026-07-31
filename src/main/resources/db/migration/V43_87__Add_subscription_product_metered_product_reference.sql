alter table if exists "subscription_product"
    add column if not exists metered_product_id varchar;

update "subscription_product"
set metered_product_id =
        (select id
         from "subscription_product"
         where consumption_type_attached = 'ROOF_ANALYSIS'
         limit 1)
where consumption_type_attached is null;
