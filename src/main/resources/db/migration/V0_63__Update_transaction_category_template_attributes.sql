alter table "transaction_category_template"
    add column "other" boolean not null default false;
alter table "transaction_category_template"
    add column description varchar;