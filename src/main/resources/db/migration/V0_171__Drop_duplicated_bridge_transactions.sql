create or replace view min_bridge_transaction_id as
(
select min(t_id) as id, id_bridge
from (select t.id as t_id, t.id_bridge
      from (select id_bridge
            from (select count(id_bridge) as req1, id_bridge
                  from transaction
                  group by id_bridge) bridge_occ_multiple
            where req1 > 1) req2
               join transaction t on t
                                         .id_bridge =
                                     req2.id_bridge) req3
group by id_bridge);

create or replace view duplicated_bridge_transactions_id as
(
select t.id, m.id_bridge
from min_bridge_transaction_id m
         join transaction t on t.id_bridge = m.id_bridge
where t.id != m.id);

create or replace view min_duplicated_bridge_transactions as
(
select m.id as min_id, d.id as dupl_id, m.id_bridge
from min_bridge_transaction_id m
         left join duplicated_bridge_transactions_id d
                   on m.id_bridge = d.id_bridge);

update transaction_category tc
set id_transaction = req.min_id
from (select * from min_duplicated_bridge_transactions) req
where id_transaction = req.dupl_id;

delete
from transaction t
where id in (select id from duplicated_bridge_transactions_id);