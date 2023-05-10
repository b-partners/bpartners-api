delete
from transaction tr
where tr.id in (select min(req2.id)
                from (select t.id, t.id_bridge
                      from transaction t
                      where t.id_bridge in (select id_bridge
                                            from (select count(*), id_bridge from transaction group by id_bridge)
                                                     as req1
                                            where req1.count > 1)
                         or id_bridge is null) as req2
                group by req2.id_bridge)
   or tr.id_bridge is null;