alter table user_subscription_session
    add column if not exists creation_datetime timestamp without time zone;