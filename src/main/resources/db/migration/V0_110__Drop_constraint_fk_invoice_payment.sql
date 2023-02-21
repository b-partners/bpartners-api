alter table payment_request
    drop constraint if exists fk_invoice_payment;
