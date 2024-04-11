create table "has_customer"
(
    id          varchar primary key default uuid_generate_v4(),
    id_prospect varchar unique not null,
    id_customer varchar unique not null,
    constraint fk_has_customer_prospect foreign key (id_prospect) references prospect (id),
    constraint fk_has_customer_customer foreign key (id_customer) references customer (id)
);