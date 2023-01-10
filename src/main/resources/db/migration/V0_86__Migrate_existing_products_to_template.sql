insert into product_template (id_account, description, unit_price, vat_percent)
select p.id_account, p.description, p.unit_price, p.vat_percent
from (select p.id_account, p.description, p.unit_price, p.vat_percent
      from product p
               join
           (select id, id_account, description, quantity, vat_percent
            from product
            group by (id, id_account, description, quantity, vat_percent)) dist_product
           on p.id = dist_product.id) p;

alter table product
    drop column id_account;