alter table if exists "subscription_product"
    add column if not exists deprecated boolean not null default false;

update "subscription_product"
set deprecated = true
where name ilike '%Abonnement essentiel%';
