create table if not exists product_template
(
    id               varchar
        constraint product_template_pk primary key default uuid_generate_v4(),
    id_account       varchar,
    description      varchar,
    unit_price       varchar,
    vat_percent      varchar,
    created_datetime timestamp not null            default current_timestamp
);