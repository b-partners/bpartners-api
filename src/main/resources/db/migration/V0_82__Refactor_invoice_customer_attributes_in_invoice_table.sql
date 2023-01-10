alter table "invoice"
    drop column id_invoice_customer,
    add column id_customer       varchar references customer (id),
    add column customer_email    varchar,
    add column customer_phone    varchar,
    add column customer_address  varchar,
    add column customer_website  varchar,
    add column customer_city     varchar,
    add column customer_zip_code integer,
    add column customer_country  varchar;

update "invoice"
set id_customer       = ic.id_customer,
    customer_email    = ic.email,
    customer_phone    = ic.phone,
    customer_address  = ic.address,
    customer_website  = ic.website,
    customer_city     = ic.city,
    customer_zip_code = cast(ic.zip_code as integer),
    customer_country  = ic.country
from (select ic.*
      from invoice_customer ic,
           (select ic.id_invoice, max(ic.created_datetime) as created_datetime
            from invoice_customer ic
            group by(ic.id_invoice)) max_ic
      where ic.id_invoice = max_ic.id_invoice
        and ic.created_datetime = max_ic.created_datetime) as ic
where invoice.id = ic.id_invoice;

drop table if exists invoice_customer;