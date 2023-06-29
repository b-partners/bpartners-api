delete from transaction
where id in (select t.id
             from (select min(id) as min_id, req3.id_bridge
                   from (select t.id, t.id_bridge
                         from (select id_bridge
                               from (select id_bridge, count(id_bridge) as occ
                                     from transaction t
                                     group by id_bridge) req1
                               where occ > 1
                               group by id_bridge) req2
                                  join transaction t on t.id_bridge = req2.id_bridge) req3
                   group by req3.id_bridge) req4
                      join transaction t on t.id_bridge = req4.id_bridge where id != req4.min_id);

alter table "transaction"
    add constraint "unique_bridge_transaction_id" unique (id_bridge);