delete
from transaction tr
where tr.id in (select min(v.id)
                from (select *
                      from transaction t
                      where t.id_bridge in (select id_bridge
                                            from (select count(*), id_bridge from transaction group by id_bridge)
                                                     as u
                                            where u.count > 1)
                         or id_bridge is null) as v
                group by v.id_bridge)
   or tr.id_bridge is null;