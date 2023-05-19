create table if not exists feedback
(
    id                varchar
        constraint feedback_pk primary key default uuid_generate_v4(),
    account_holder_id varchar,
    creation_datetime timestamp            default current_timestamp,
    constraint account_holder_fk foreign key (account_holder_id) references "account_holder"(id)
);