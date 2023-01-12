alter table "invoice"
    add column delay_in_payment_allowed integer;
alter table "invoice"
    add column delay_penalty_percent varchar;