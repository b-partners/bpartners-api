alter table "account"
    drop column if exists status;

drop type if exists account_status;
