alter table "payment_request"
    add column if not exists user_updated              boolean,
    add column if not exists payment_status_updated_at timestamp;