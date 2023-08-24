alter table "payment_request"
    add column if not exists user_updated              boolean,
    add column if not exists payment_method            payment_method_type,
    add column if not exists payment_status_updated_at timestamp;

update "payment_request"
set payment_method = 'UNKNOWN'
where status = 'PAID'
  and payment_method is null;