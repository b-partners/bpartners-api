create table if not exists "file_info"
(
    id varchar
    constraint file_pk primary key default uuid_generate_v4(),
    uploaded_at timestamp with time zone not null,
    id_user varchar not null
        constraint file_fk_user references "user"(id),
    size_in_kb integer not null,
    sha256 varchar not null
);
