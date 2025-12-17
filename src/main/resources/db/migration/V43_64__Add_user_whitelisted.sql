create table if not exists "user_whitelisted"
(
    id                varchar
        constraint user_whitelisted_pk primary key default uuid_generate_v4(),
    user_id           varchar not null,
    creation_datetime timestamp without time zone  default current_timestamp,
    constraint user_whitelisted_fk foreign key (user_id) references "user" (id)
);

create index if not exists "index_user_whitelisted" on "user_whitelisted" (user_id);