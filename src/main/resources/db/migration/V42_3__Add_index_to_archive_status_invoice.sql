create index if not exists "invoice_user_index" ON "invoice"
    ("id_user");
create index if not exists "invoice_archive_status_index" ON "invoice"
    ("archive_status");