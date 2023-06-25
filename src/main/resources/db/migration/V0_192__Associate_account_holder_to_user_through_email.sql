update "account_holder" ah
set id_user = id_u
from (select u.id as id_u, a.id as id_a, u.email
      from "user" u
               join account_holder a on u.email = a.email) ahview
where ah.id = id_a;