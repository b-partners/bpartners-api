alter table "invoice"
    drop constraint "invoice_ref_unique";
alter table "invoice"
    add constraint "invoice_ref_and_status_unique"
        unique (ref, status);
alter table "invoice"
    add column "to_be_relaunched" boolean
        check (
                    to_be_relaunched = false
                or
                    (to_be_relaunched = true
                        AND
                     ("invoice".status in ('PROPOSAL', 'CONFIRMED'))
                        )
            )
        default false;