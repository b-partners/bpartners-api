alter table "invoice_customer"
    add column created_datetime timestamp default current_timestamp;