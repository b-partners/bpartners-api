alter table if exists "subscription_product"
    add column if not exists most_chosen boolean not null default false;

update "subscription_product"
set most_chosen = true
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';