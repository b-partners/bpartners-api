alter type subscription_upgrade_status add value if not exists 'AWAITING_PAYMENT';
alter type subscription_upgrade_status add value if not exists 'EXPIRED';

alter table if exists "user_subscription_upgrade"
    add column if not exists old_stripe_subscription_id varchar,
    add column if not exists payment_url varchar,
    add column if not exists payment_expiration_datetime timestamp without time zone;
