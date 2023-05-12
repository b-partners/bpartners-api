delete from transaction tr
where tr.id in (select req2.id
                from (select t.id, t.id_bridge
                      from transaction t
                      where t.id_bridge in (select id_bridge
                                            from (select count(*), id_bridge from transaction group by id_bridge)
                                                     as req1
                                            where req1.count > 1)
                         or id_bridge is null) as req2);