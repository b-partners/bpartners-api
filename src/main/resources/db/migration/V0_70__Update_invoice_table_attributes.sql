alter table "invoice"
    drop constraint "invoice_ref_unique";
alter table "invoice"
    add constraint "invoice_ref_and_status_unique"
        unique (ref, status);