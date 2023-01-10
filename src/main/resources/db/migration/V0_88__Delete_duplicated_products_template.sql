delete
from product_template
where id not in (select p.id
                 from product_template p
                          join (select max(id) as id,
                                       id_account,
                                       description,
                                       unit_price,
                                       vat_percent,
                                       max(created_datetime)
                                               as created_datetime
                                from product_template
                                group by (id_account, description, unit_price,
                                          vat_percent)) pt
                               on p.id = pt.id);