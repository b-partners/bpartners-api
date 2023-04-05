alter table "user"
    add column if not exists email varchar;

update "user"
set email = ac.email
from (select ac.email as email, u.id as u_id
      from "account_holder" ac
               join
           account a on ac.account_id = a.id
               join "user" u on a.id_user = u.id) as ac
where "user".id = u_id;