create table if not exists "price_reduction"
(
    id varchar
    constraint price_reduction_pk primary key default uuid_generate_v4(),
    description varchar not null,
    "value" integer not null
);
