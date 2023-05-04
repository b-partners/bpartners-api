alter table "monthly_transactions_summary"
    add column id_user varchar;

update "monthly_transactions_summary"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join monthly_transactions_summary m on m.id_account = a.id) u_a
where u_a.a_id = "monthly_transactions_summary".id_account;