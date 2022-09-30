alter table "product"
    drop column id_invoice,
    add column id_invoice_product varchar,
    add constraint id_invoice_product_fk foreign key(id_invoice_product)
        references invoice_product(id);