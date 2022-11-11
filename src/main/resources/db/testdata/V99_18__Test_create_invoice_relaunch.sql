insert into "invoice_relaunch"
    (id,"type", id_invoice, is_user_relaunched, creation_datetime)
values ('invoice_relaunch1_id','PROPOSAL', 'invoice1_id', true, '2022-01-01T01:00:00.00Z'),
       ('invoice_relaunch2_id','CONFIRMED', 'invoice1_id', false, '2022-01-01T01:00:00.00Z');