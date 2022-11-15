alter table "transaction_category"
    add constraint fk_transaction_category foreign key (id_transaction) references "transaction"("swan_id");
alter table "transaction_category"
    add column "comment" varchar;
alter table "transaction_category"
    drop column "vat";
alter table "transaction_category"
    add column "description" varchar;