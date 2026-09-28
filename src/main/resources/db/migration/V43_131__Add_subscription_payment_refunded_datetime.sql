alter table subscription_payment
    add column if not exists refunded_datetime timestamp without time zone;

create index if not exists subscription_payment_refunded_datetime_idx
    on subscription_payment (refunded_datetime);
