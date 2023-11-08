update "payment_request"
set payment_status_updated_at = created_datetime
where payment_status_updated_at is null;