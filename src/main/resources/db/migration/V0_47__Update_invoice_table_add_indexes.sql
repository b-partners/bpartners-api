create index "invoice_account_index" ON "invoice"
    (id_account);
create index "invoice_ref_index" ON "invoice"
    ("ref");
create index "invoice_status_index" ON "invoice"
    (status);
