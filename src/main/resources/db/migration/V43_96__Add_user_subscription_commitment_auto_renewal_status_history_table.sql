create table if not exists user_subscription_commitment_auto_renewal_status_history
(
    id                              varchar primary key default uuid_generate_v4(),
    user_subscription_commitment_id varchar not null,
    auto_renewal_status             enable_status,
    creation_datetime               timestamp without time zone,
    foreign key (user_subscription_commitment_id) references user_subscription_commitment (id)
);