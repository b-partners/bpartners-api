create table if not exists "transaction_category_template"
(
    id varchar
    constraint transaction_category_template_pk primary key default uuid_generate_v4(),
    "type" varchar not null,
     vat integer not null
);
