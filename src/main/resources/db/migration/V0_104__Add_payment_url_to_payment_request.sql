alter table payment_request
    add column if not exists payment_url varchar;