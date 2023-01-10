alter table "invoice"
    add column id_invoice_product varchar references invoice_product (id);

update invoice
set id_invoice_product = ip.id
from (select ip.*
      from invoice_product ip,
           (select ip.id_invoice, max(ip.created_datetime) as created_datetime
            from invoice_product ip
            group by (ip.id_invoice)) max_ip
      where ip.id_invoice = max_ip.id_invoice
        and ip.created_datetime = max_ip.created_datetime) as ip
where invoice.id = ip.id_invoice;

delete from product
where id_invoice_product not in
      (select ip.id
       from invoice_product ip,
            (select ip.id_invoice, max(ip.created_datetime) as created_datetime
             from invoice_product ip
             group by (ip.id_invoice)) max_ip
       where ip.id_invoice = max_ip.id_invoice
         and ip.created_datetime = max_ip.created_datetime);

delete
from invoice_product
where id not in
      (select ip.id
       from invoice_product ip,
            (select ip.id_invoice, max(ip.created_datetime) as created_datetime
             from invoice_product ip
             group by (ip.id_invoice)) max_ip
       where ip.id_invoice = max_ip.id_invoice
         and ip.created_datetime = max_ip.created_datetime);


alter table product
    add column id_invoice varchar references invoice (id);

update product
set id_invoice = ip.id_invoice
from (select i.id as id_invoice, i.id_invoice_product as id
      from invoice_product ip
               join invoice i on ip.id = i.id_invoice_product) as ip
where ip.id = product.id_invoice_product;

alter table invoice
    drop column id_invoice_product;

alter table product
    drop column id_invoice_product;

