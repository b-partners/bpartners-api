alter table "invoice"
    drop column vat,
    drop constraint invoice_customer_fk,
    drop column id_customer,
    add column id_invoice_customer varchar,
    add constraint inv_invoice_customer_fk foreign key(id_invoice_customer)
        references invoice_customer(id);