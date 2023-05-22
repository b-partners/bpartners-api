alter table "user"
    rename "preferred_account_external_id" to "preferred_account_id";

update "user"
set preferred_account_id = a_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
      where u.id = a.id_user
        and u.preferred_account_id = a.external_id) u_a
where u_a.u_id = "user".id;