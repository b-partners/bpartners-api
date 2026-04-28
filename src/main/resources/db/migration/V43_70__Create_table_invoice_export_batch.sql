create table "invoice_export_request"
(
    id                  varchar default uuid_generate_v4() primary key,
    user_id             varchar
        constraint fk_invoice_export_request_user_id references "user" (id),
    begin_date          date,
    end_date            date,
    batch_size          int,
    total_batch_count   int,
    total_invoice_count int,
    status_list         jsonb,
    archive_status      archive_status,
    creation_datetime   timestamp without time zone
);

create index "invoice_export_request_user_id_idx" on "invoice_export_request" ("user_id");
create index "invoice_export_begin_date_idx" on "invoice_export_request" ("begin_date");
create index "invoice_export_end_date_idx" on "invoice_export_request" ("end_date");
create index "invoice_export_status_list_idx" on "invoice_export_request" ("status_list");
create index "invoice_export_archive_status_idx" on "invoice_export_request" ("archive_status");