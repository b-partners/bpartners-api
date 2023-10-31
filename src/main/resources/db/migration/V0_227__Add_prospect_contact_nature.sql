alter table "prospect"
    add column if not exists contact_nature contact_nature;

create or replace view customer_names as
(
select fn || ' ' || ln as name1, ln || ' ' || fn as name2
from (select lower(first_name) as fn, lower(last_name) as ln from customer) as req1);

create or replace view prospect_lower_names as
(
select p.id as prospect_id, lower(p.old_name) as name, lower(p.manager_name) as manager_name
from prospect p);

update "prospect"
set contact_nature = 'OLD_CUSTOMER'
from (select prospect_id
      from prospect_lower_names pln
               join customer_names cn
                    on pln.name like '%' || cn.name1 || '%'
                        or pln.name like '%' || cn.name2 || '%'
                        or pln.manager_name like '%' || cn.name1 || '%'
                        or pln.manager_name like '%' || cn.name2 || '%') as req
where "prospect".id = prospect_id;

update "prospect"
set contact_nature = 'PROSPECT'
where contact_nature is null;