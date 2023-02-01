create table "attachment"
(
    id                  varchar
        constraint attachment_pk primary key default uuid_generate_v4(),
    name                varchar unique not null,
    id_invoice_relaunch varchar,
    constraint fk_relaunch foreign key (id_invoice_relaunch) references invoice_relaunch (id),
    id_file             varchar,
    constraint fk_file foreign key (id_file) references file_info (id)
);