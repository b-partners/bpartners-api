create extension if not exists "uuid-ossp";


create table if not exists "account_holder"
(
    id                   varchar
        constraint account_holder_pk primary key        default uuid_generate_v4(),
    mobile_phone_number         varchar     not null,
    email         varchar     not null,
    account_id         varchar     not null,
    social_capital         varchar     not null,
    tva_number         varchar     not null
);
