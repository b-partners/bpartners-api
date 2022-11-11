alter table "transaction_category"
    add constraint fk_transaction_category foreign key (id_transaction) references "transaction"("swan_id");