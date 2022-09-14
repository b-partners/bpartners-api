alter table "invoice" rename column invoice_date TO sending_date;
alter table "invoice" drop column percentage_reduction;
alter table "invoice" drop column amount_reduction;