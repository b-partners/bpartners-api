alter table "account"
    add column if not exists external_id varchar;

update "account"
set external_id = cast("account".bridge_account_id as varchar)
where "account".bridge_account_id is not null;

alter table "account"
    drop column if exists bridge_account_id;