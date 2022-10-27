create table if not exists "account_holder"
(
    id varchar
    constraint account_holder_pk primary key default uuid_generate_v4(),
    mobile_phone_number varchar not null,
    email varchar,
    account_id varchar,
    social_capital integer,
    tva_number varchar );