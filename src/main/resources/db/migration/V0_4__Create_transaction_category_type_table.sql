create table if not exists "transaction_category_type"
(
    id varchar
    constraint transaction_category_type_pk primary key default uuid_generate_v4(),
    label varchar not null);
