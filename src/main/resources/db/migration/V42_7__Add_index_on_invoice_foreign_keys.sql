create index if not exists "invoice_file_index" ON "invoice"("file_id");
create index if not exists "invoice_customer_index" ON "invoice"("id_customer");