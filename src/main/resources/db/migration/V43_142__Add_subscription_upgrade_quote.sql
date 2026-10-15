update user_subscription_upgrade_status_history
set status = 'PROCESSING'
where status = 'PENDING';

alter table if exists "user_subscription_upgrade"
    add column if not exists confirmation_datetime timestamp without time zone,
    add column if not exists quote_expiration_datetime timestamp without time zone,
    add column if not exists new_charge_in_cents_with_vat bigint,
    add column if not exists credited_in_cents_with_vat bigint;

update user_subscription_upgrade
set confirmation_datetime = creation_datetime
where confirmation_datetime is null;

create table if not exists user_subscription_upgrade_line
(
    id                                varchar primary key default uuid_generate_v4(),
    user_subscription_upgrade_id      varchar                     not null,
    line_order                        integer                     not null,
    kind                              varchar                     not null,
    subscription_product_id           varchar                     not null,
    billing_interval                  billing_interval            not null,
    period_start_date                 date                        not null,
    period_end_date                   date                        not null,
    days                              integer                     not null,
    reference_period_days             integer                     not null,
    reference_price_in_cents_with_vat bigint                      not null,
    amount_in_cents_with_vat          bigint                      not null,
    foreign key (user_subscription_upgrade_id) references user_subscription_upgrade (id) on delete cascade,
    foreign key (subscription_product_id) references subscription_product (id)
);

create index if not exists user_subscription_upgrade_line_upgrade_id_idx
    on user_subscription_upgrade_line (user_subscription_upgrade_id);
