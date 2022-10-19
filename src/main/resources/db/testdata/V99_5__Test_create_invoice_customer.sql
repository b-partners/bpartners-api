insert into "invoice_customer"
    (id, id_customer, id_invoice, address, created_datetime)
values ('invoice_customer1_id', 'customer2_id', 'invoice1_id', null, '2022-01-01T00:00:00.00Z'),
       ('invoice_customer2_id', 'customer1_id', 'invoice1_id', null, '2022-01-01T10:00:00.00Z'),
       ('invoice_customer3_id', 'customer2_id', 'invoice2_id', 'Nouvelle adresse',
        '2022-01-01T01:00:00.00Z'),
       ('invoice_customer4_id', 'customer1_id', 'invoice3_id', null,
        '2022-01-01T01:00:00.00Z'),
       ('invoice_customer5_id', 'customer1_id', 'invoice4_id', null,
        '2022-01-01T01:00:00.00Z'),
       ('invoice_customer6_id', 'customer1_id', 'invoice5_id', null,
        '2022-01-01T01:00:00.00Z');