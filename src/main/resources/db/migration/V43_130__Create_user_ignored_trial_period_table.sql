create table if not exists user_ignored_trial_period
(
    id                varchar primary key                 default uuid_generate_v4(),
    user_id           varchar                     not null,
    creation_datetime timestamp without time zone         default current_timestamp,
    foreign key (user_id) references "user" (id),
    unique (user_id)
);

create index if not exists user_ignored_trial_period_user_id_idx
    on user_ignored_trial_period (user_id);
