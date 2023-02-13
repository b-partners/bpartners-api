alter table payment_request
    add column if not exists payment_due_date date;
