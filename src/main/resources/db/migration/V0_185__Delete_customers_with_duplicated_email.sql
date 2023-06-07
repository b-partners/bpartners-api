update invoice i
set id_customer = req.min_id_cust
from (select c.id as other_id, min_id_cust
      from customer c
               join
           (select min(id) as min_id_cust, c.id_user, c.email
            from customer c
                     join (select email, id_user
                           from (select count(email) as occ, email, id_user
                                 from customer
                                 group by id_user, email) req1
                           where occ > 1) req2 on c.id_user = req2.id_user and c.email = req2.email
            group by c.id_user, c.email) req on c.id_user = req.id_user and c.email = req.email
               and c.id != req.min_id_cust) req
where id_customer = req.other_id;

delete
from customer
where id in (select c.id as other_id
             from customer c
                      join
                  (select min(id) as min_id_cust, c.id_user, c.email
                   from customer c
                            join (select email, id_user
                                  from (select count(email) as occ, email, id_user
                                        from customer
                                        group by id_user, email) req1
                                  where occ > 1) req2
                                 on c.id_user = req2.id_user and c.email = req2.email
                   group by c.id_user, c.email) req
                  on c.id_user = req.id_user and c.email = req.email
                      and c.id != req.min_id_cust);