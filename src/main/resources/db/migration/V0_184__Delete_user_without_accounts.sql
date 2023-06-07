create or replace view user_without_accounts as
(
select u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               left join account a on u.id = a.id_user) req
where a_id is null);

delete
from "user_legal_file"
where id_user in (select u_id
                  from user_without_accounts);

delete
from "user"
where id in (select u_id
             from user_without_accounts);