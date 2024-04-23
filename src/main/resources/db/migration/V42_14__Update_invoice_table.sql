alter table invoice
    add column "id_area_picture" varchar;
alter table invoice
    add constraint "fk_invoice_area_picture" foreign key (id_area_picture) references area_picture (id);