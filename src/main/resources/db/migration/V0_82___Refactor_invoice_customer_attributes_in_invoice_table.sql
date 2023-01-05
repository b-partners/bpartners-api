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
      from invoice_customer ic
               join invoice i on ic.id_invoice = i.id
      where ic.id_invoice = i.id
      order by created_datetime desc
      limit 1) as ic;

drop table if exists invoice_customer;