update invoice
set id_customer = s3.org_id from
(select id_invoice, org_id
from (select i.id          as id_invoice,
             first_name    as c_fn,
             last_name     as c_ln,
             email         as c_e
      from invoice i
               inner join customer c
                          on c.id = i.id_customer
      order by i.id desc) as s1
         join
     (select max(id) as org_id, first_name, last_name, email
      from customer
      group by first_name, last_name, email) as s2
     on (c_e = email or c_e is null) and (c_fn = first_name or c_fn is null) and (c_ln = last_name or c_ln is null)) as s3
where "invoice".id = s3.id_invoice;


delete
from customer
where id not in ((select max(id) as id
                  from customer
                  group by first_name, last_name, email))