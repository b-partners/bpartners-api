alter table "transaction_category"
    add column created_datetime timestamp not null default current_timestamp;