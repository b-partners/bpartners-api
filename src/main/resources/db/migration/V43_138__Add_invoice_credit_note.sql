alter table if exists "invoice"
    add column if not exists credit_note boolean default false;

update "invoice"
set credit_note = true
where id in (select credit_note_invoice_id
             from user_subscription_upgrade
             where credit_note_invoice_id is not null);
