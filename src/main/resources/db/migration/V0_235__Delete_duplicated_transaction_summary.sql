delete
from monthly_transactions_summary
where id in (select id
             from (select max(id) as unique_id, mts.id_user, mts.year, mts.month
                   from (select *
                         from (select count(id) as count, id_user, year, month
                               from monthly_transactions_summary
                               group by (id_user, year, month)) req1
                         where count > 1) req2
                            join monthly_transactions_summary mts
                                 on mts.id_user = req2.id_user
                                     and mts.year = req2.year
                                     and mts.month = req2.month
                   group by (mts.id_user, mts.year, mts.month)) req3
                      join monthly_transactions_summary mts2 on mts2.id_user = req3.id_user
                 and mts2.year = req3.year
                 and mts2.month = req3.month
             where mts2.id <> req3.unique_id);