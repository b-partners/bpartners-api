alter table payment_request add constraint fk_payment_invoice
    foreign key (id_invoice) references invoice(id);