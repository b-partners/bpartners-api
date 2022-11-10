create table if not exists "email_message"
(
    id                       varchar
        constraint email_message_pk primary key default uuid_generate_v4(),
    message   varchar not null,
    id_account varchar not null,
    constraint email_unique_account_id unique(id_account)
);