create or replace view user_accounts_number as
(
select count(a.id) as a_id_occ, u.id as u_id
from account a
         join "user" u on a.id_user = u.id
group by u.id);

-- set unique account as default preferred account in user
update "user" u
set preferred_account_id = req.id
from (select a.id as id, u_id
      from account a
               join (select u_id
                     from user_accounts_number req1
                     where a_id_occ = 1) req1 on a.id_user = req1.u_id) req
where u.preferred_account_id is null
  and u.id = req.u_id;

-- choose one account from multiple associated as default preferred account in user
update "user" u
set preferred_account_id = req.id
from (select min(a.id) as id, u_id
      from account a
               join (select u_id
                     from user_accounts_number req1
                     where a_id_occ > 1) req1 on a.id_user = req1.u_id
      group by u_id) req
where u.preferred_account_id is null
  and u.id = req.u_id;