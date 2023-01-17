alter table "account_holder"
    add column if not exists subject_to_vat boolean not null default false;