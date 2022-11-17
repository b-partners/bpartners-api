create table if not exists "legal_file"
(
    id   varchar
    constraint legal_file_pk primary key default uuid_generate_v4(),
    "name" varchar,
    file_url varchar,
    constraint legal_file_name_unique unique("name")
)
