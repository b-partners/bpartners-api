create table if not exists subscription_product_included_feature
(
    subscription_product_id          varchar not null,
    included_subscription_product_id varchar not null,
    primary key (subscription_product_id, included_subscription_product_id),
    foreign key (subscription_product_id) references subscription_product (id),
    foreign key (included_subscription_product_id) references subscription_product (id),
    check (subscription_product_id <> included_subscription_product_id)
);

insert into subscription_product_included_feature (subscription_product_id,
                                                   included_subscription_product_id)
values ('c5f57306-a7b1-43f4-90fc-204ccd4c0ce2', '89f1acdd-c3b9-4717-a21d-355b2021ad58'),
       ('37b9639e-d058-4222-8a2a-d78d5fe7b6b1', 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2')
on conflict do nothing;
