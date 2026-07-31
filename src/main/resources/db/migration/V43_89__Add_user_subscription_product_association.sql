create table if not exists "user_subscription_product"
(
    id                          varchar
        constraint user_subscription_product_pk primary key default uuid_generate_v4(),
    user_id                     varchar not null,
    subscription_product_id     varchar not null,
    subscription_start_datetime timestamp without time zone,
    subscription_end_datetime   timestamp without time zone,
    creation_datetime           timestamp without time zone,
    constraint user_subscription_product_fk foreign key (user_id) references "user" (id),
    constraint subscription_product_id_fk foreign key (subscription_product_id) references subscription_product (id)
);
