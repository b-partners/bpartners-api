
create index if not exists "payment_request_invoice_index" ON "payment_request"("id_invoice");
create index if not exists "payment_request_user_index" ON "payment_request"("id_user");