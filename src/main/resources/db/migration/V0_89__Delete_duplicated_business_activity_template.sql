delete from business_activity_template
where id in (select bat.id from business_activity_template bat,(
    select max(id) as id, lower(name) as name
    from (select id, bat.name
          from business_activity_template bat,
               (select lower_name
                from (select count
                                 (lower(name)) as count,
                             lower
                                 (name)        as
                                                  lower_name
                      from business_activity_template
                      group by lower
                                   (name)) b
                where count > 1) b
          where lower(bat.name) = lower_name) bat
    group by lower(name)) b where b.id = bat.id);