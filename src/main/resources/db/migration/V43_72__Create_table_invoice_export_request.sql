create table if not exists "invoice_export_batch"
(
    id                        varchar default uuid_generate_v4() primary key,
    invoice_export_request_id varchar
        constraint fk_invoice_export_request references "invoice_export_request" (id),
    file_key                  varchar,
    properties                jsonb,
    content_size              int,
    creation_datetime         timestamp without time zone
);