create table if not exists "pre_registration"
(
    id varchar
    constraint pre_registration_pk primary key default uuid_generate_v4(),
    first_name varchar,
    last_name varchar,
    society varchar,
    email varchar not null,
    phone_number varchar,
    entrance_datetime timestamp with time zone not null
    );
create index if not exists pre_registration_email_index on "pre_registration" (email);
