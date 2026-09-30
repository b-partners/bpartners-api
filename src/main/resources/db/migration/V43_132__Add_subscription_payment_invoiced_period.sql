alter table subscription_payment
    add column if not exists invoiced_period_start_datetime timestamp without time zone,
    add column if not exists invoiced_period_end_datetime timestamp without time zone;

update subscription_payment p
set invoiced_period_start_datetime =
        to_date(substring(i.title from 'du (\d{2}/\d{2}/\d{4}) au'), 'DD/MM/YYYY')::timestamp,
    invoiced_period_end_datetime =
        to_date(substring(i.title from 'au (\d{2}/\d{2}/\d{4})'), 'DD/MM/YYYY')::timestamp
from "invoice" i
where i.id = p.invoice_id
  and p.invoiced_period_end_datetime is null
  and i.title ~ 'du \d{2}/\d{2}/\d{4} au \d{2}/\d{2}/\d{4}';
