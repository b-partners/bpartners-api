alter table "transaction_category"
    drop column user_defined;
alter table "transaction_category"
    add column id_transaction_category_tmpl varchar;
alter table "transaction_category"
    add constraint category_template_fk foreign key (id_transaction_category_tmpl) references
        "transaction_category_template" (id);