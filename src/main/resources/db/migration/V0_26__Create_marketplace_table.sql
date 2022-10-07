create table if not exists "marketplace"(
    id varchar
    constraint marketplace_pk primary key default uuid_generate_v4(),
    "name" varchar,
    description varchar,
    phone_number varchar,
    website_url varchar,
    logo_url varchar,
    account_id varchar
);