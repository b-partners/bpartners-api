create table if not exists "invoice_customer"
(
    id varchar
    constraint invoice_customer_pk primary key default uuid_generate_v4(),
    id_customer varchar,
    id_invoice varchar,
    email varchar,
    phone varchar,
    address varchar,
    website varchar,
    city varchar,
    zip_code varchar,
    country varchar,
    constraint invoice_customer_fk foreign key(id_customer)
    references "customer"(id),
    constraint invoice_invoice_customer_fk foreign key(id_invoice)
    references "invoice"(id)
    );
