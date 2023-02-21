alter table invoice
    add column if not exists discount_percent varchar default '0/1';