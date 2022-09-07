create table if not exists "transaction_category"
(
    id varchar
    constraint transaction_category_pk primary key default uuid_generate_v4(),
    comment varchar,
    id_transaction_category_type varchar not null,
    id_transaction varchar not null,
    constraint transaction_category_fk foreign key(id_transaction_category_type) references
    "transaction_category_type" (id));
