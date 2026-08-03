drop index if exists "user_bridge_id_index";
drop index if exists "user_token_index";

alter table "user"
    drop column if exists bridge_user_id,
    drop column if exists bridge_password,
    drop column if exists access_token,
    drop column if exists token_expiration_datetime,
    drop column if exists token_creation_datetime,
    drop column if exists monthly_subscription,
    drop column if exists id_verified,
    drop column if exists identification_status;

drop type if exists identification_status;
