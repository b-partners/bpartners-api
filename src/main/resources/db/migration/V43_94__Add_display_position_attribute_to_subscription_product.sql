alter table if exists "subscription_product"
    add column if not exists display_position integer;

update "subscription_product"
set display_position = 1
where id = '4219611e-7584-4636-a3c5-ba212600715b';

update "subscription_product"
set display_position = 2
where id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';

update "subscription_product"
set display_position = 3
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';

update "subscription_product"
set display_position = 4
where id = '37b9639e-d058-4222-8a2a-d78d5fe7b6b1';