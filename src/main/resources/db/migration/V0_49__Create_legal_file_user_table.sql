create table if not exists "user_legal_file"
(
    id   varchar
    constraint user_legal_file_pk primary key default uuid_generate_v4(),
    id_user varchar,
    id_legal_file varchar,
    approval_datetime timestamp with time zone default current_timestamp,
    constraint user_legal_file_from_user_fk
    foreign key(id_user) references "user"(id),
    constraint user_legal_file_from_legal_file_fk
    foreign key(id_legal_file) references "legal_file"(id)
);
