update invoice
set created_datetime = updated_at
where created_datetime is null;

alter table invoice
    alter
        column created_datetime set not null;