alter table payment_request
    add constraint fk_invoice_payment foreign key (id_invoice) references invoice (id);