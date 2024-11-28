create table if not exists user_subscription_eligible
(
    id      varchar primary key,
    user_id varchar references "user" (id)
);
