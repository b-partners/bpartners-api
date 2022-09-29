create table if not exists "invoice_product"
(
    id varchar
    constraint invoice_product_pk primary key default uuid_generate_v4(),
    id_invoice varchar,
    created_datetime timestamp with time zone not null default current_timestamp,
    constraint inv_product_rel_fk foreign key(id_invoice)
        references invoice(id)
);
