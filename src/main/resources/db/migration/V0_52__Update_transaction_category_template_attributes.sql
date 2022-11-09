alter table "transaction_category_template"
    add column "other" boolean default false;
alter table "transaction_category_template"
    add column "transaction_type" transaction_type not null;
alter table "transaction_category_template"
    drop column "vat";