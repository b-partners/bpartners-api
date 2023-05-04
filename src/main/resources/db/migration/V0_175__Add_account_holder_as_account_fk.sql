alter table "account_holder"
    add column if not exists id_user varchar;

update "account_holder" ah
set id_user = ac_ah.id_user
from (select ac.id as id_a, h.id as id_ah, ac.id_user
      from account ac
               join account_holder h on ac.id = h.account_id) ac_ah
where ah.id = ac_ah.id_ah;

alter table "account"
    add column if not exists id_account_holder varchar;

update "account" a
set id_account_holder = ah.id_ah
from (select ah.id as id_ah, ah.account_id as id_a
      from account_holder ah
               join account ac on ah.account_id = ac.id) ah
where a.id = ah.id_a;

alter table "account_holder"
    drop column if exists account_id;