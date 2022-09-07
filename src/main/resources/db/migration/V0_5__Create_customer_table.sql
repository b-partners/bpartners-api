create table if not exists "customer"
(
    id varchar
    constraint customer_pk primary key default uuid_generate_v4(),
    id_account varchar not null,
    "name" varchar not null,
    email varchar not null,
    phone varchar not null,
    address varchar not null
);
