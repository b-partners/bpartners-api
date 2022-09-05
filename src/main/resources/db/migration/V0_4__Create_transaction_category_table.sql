create table if not exists "transaction_category"
(
    id varchar
    constraint transaction_category_pk primary key default uuid_generate_v4(),
    label varchar not null);
