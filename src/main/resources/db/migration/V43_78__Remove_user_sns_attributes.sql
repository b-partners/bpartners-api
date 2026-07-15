alter table if exists "user"
    drop column if exists device_token;
alter table if exists "user"
    drop column if exists sns_arn;