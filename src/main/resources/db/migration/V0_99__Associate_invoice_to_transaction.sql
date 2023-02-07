alter table transaction
    add column id_invoice varchar references invoice(id);
