alter table "payment_request"
    alter column payer_name drop not null,
    alter column payer_email drop not null;