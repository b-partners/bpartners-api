create table if not exists subscription_payment
(
    id                          varchar primary key         default uuid_generate_v4(),
    user_id                     varchar not null,
    stripe_invoice_id           varchar not null,
    stripe_subscription_id      varchar,
    subscription_product_id     varchar,
    billing_interval            billing_interval,
    label                       varchar,
    amount_in_cents_without_vat bigint,
    amount_in_cents_with_vat    bigint,
    vat_percent                 bigint,
    period_start_datetime       timestamp without time zone,
    period_end_datetime         timestamp without time zone,
    payment_datetime            timestamp without time zone,
    invoice_id                  varchar,
    creation_datetime           timestamp without time zone default current_timestamp,
    foreign key (user_id) references "user" (id),
    foreign key (subscription_product_id) references subscription_product (id)
);

create unique index if not exists subscription_payment_stripe_invoice_id_unique_idx
    on subscription_payment (stripe_invoice_id);

create index if not exists subscription_payment_user_id_idx on subscription_payment (user_id);
