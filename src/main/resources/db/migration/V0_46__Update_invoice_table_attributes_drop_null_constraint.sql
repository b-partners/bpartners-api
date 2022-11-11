alter table "invoice" alter column "ref" drop not null;
alter table "invoice" alter column title drop not null;
alter table "invoice" alter column sending_date drop not null;
alter table "invoice" alter column to_pay_at drop not null;
