alter table "user"
    add column if not exists "old_s3_id_account" varchar;

update "user"
set "old_s3_id_account" = req.id_account
from (select a.id as id_account, u.id as id_user
      from account a
               join "user" u on a.id_user = u.id) req
where "user".id = req.id_user;