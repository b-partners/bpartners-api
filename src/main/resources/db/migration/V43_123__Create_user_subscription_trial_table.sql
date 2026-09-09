create table if not exists user_subscription_trial
(
    id                      varchar primary key                 default uuid_generate_v4(),
    user_id                 varchar                     not null,
    subscription_product_id varchar                     not null,
    started_at              timestamp without time zone not null,
    expires_at              timestamp without time zone not null,
    creation_datetime       timestamp without time zone         default current_timestamp,
    foreign key (user_id) references "user" (id),
    foreign key (subscription_product_id) references subscription_product (id),
    unique (user_id, subscription_product_id)
);

create index if not exists user_subscription_trial_user_id_idx
    on user_subscription_trial (user_id);
