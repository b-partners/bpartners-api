delete
from transaction_category
where id_transaction not in
      (select t.id
       from "transaction" t
                inner join (select max(dt.id) as id, dt.id_swan
                            from "transaction" t,
                                 (select max(id) as id, id_swan
                                  from "transaction"
                                  group by id_swan) dt
                            where t.id_swan = dt.id_swan
                            group by dt.id_swan) as subquery on subquery.id = t.id);
delete
from transaction
where id not in
      (select t.id
       from "transaction" t
                inner join (select max(dt.id) as id, dt.id_swan
                            from "transaction" t,
                                 (select max(id) as id, id_swan
                                  from "transaction"
                                  group by id_swan) dt
                            where t.id_swan = dt.id_swan
                            group by dt.id_swan) as subquery on subquery.id = t.id);