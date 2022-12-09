create table if not exists "invoice_relaunch_conf"(
    id varchar constraint invoice_relaunch_conf_pk primary key default uuid_generate_v4(),
    id_invoice varchar,
    constraint invoice_relaunch_invoice_fk foreign key (id_invoice) references invoice(id),
    delay int not null,
    rehearsal_number int not null
);