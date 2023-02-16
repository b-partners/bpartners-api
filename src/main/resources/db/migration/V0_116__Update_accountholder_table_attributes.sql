do
$$
    begin
        if not exists(select from pg_type where typname = 'verification_status') then
            create type verification_status as enum ('VERIFIED', 'PENDING', 'NOT_STARTED', 'WAITING_FOR_INFORMATION');
        end if;
    end
$$;

alter table "account_holder" add column verification_status verification_status;
alter table "account_holder" add column "name" varchar;
alter table "account_holder" add column registration_number varchar;
alter table "account_holder" add column business_activity varchar;
alter table "account_holder" add column business_activity_description varchar;
alter table "account_holder" add column address varchar;
alter table "account_holder" add column city varchar;
alter table "account_holder" add column country varchar;
alter table "account_holder" add column postal_code varchar;