delete from account a
where a.id in (select req2.id
               from (select a.id
                      from account a
                      where a.external_id in (select external_id
                                            from (select count(*), external_id from account group by external_id)
                                                     as req1
                                            where req1.count > 1)
                         or external_id is null) as req2);