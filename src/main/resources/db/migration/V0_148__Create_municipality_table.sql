create extension if not exists postgis;
create extension if not exists postgis_topology;
create table if not exists "municipality"(
    id                   varchar
    constraint municipality_pk primary key        default uuid_generate_v4(),
    code varchar,
    name varchar,
    coordinates geometry
);

