alter table "transaction_category" drop column comment;
alter table "transaction_category" add column "type" varchar;
alter table "transaction_category" add column vat integer;
alter table "transaction_category" add column user_defined boolean default false;