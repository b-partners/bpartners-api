create table if not exists "invoice_content"
(
    id varchar
    constraint invoice_content_pk primary key default uuid_generate_v4(),
    description varchar not null,
    quantity integer not null,
    price integer not null,
    id_price_reduction varchar not null,
    id_invoice varchar not null,
    constraint invoice_content_fk foreign key(id_invoice)
    references "invoice"(id),
    constraint invoice_content_reduction foreign key(id_price_reduction)
    references "price_reduction"(id)
);
