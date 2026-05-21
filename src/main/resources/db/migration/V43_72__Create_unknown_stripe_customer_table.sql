create table if not exists unknown_stripe_customer
(
    id                 varchar primary key         default uuid_generate_v4(),
    stripe_customer_id varchar not null,
    name               varchar,
    email              varchar,
    phone              varchar,
    address            varchar,
    creation_datetime      timestamp without time zone default now()
);