alter table "user"
    drop column if exists "swan_user_id";
alter table "account"
    drop column if exists "id_account_holder";