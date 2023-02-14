insert into "invoice_product"
(id, description, quantity, unit_price, vat_percent, id_invoice,
 created_datetime)
values ('product1_id', 'Tableau malgache', 1, '1000/1', '2000/1',
        'invoice4_id', '2022-01-01T01:00:00.00Z'),
       ('product2_id', 'Tableau baobab', 2, '2000/1', '1000/1',
        'invoice5_id', '2022-01-01T02:00:00.00Z'),
       ('product3_id', 'Tuyau 1m', 3, '2000/1', '1000/1',
        'invoice1_id', '2022-01-01T03:00:00.00Z'),
       ('product4_id', 'Autres produits', 1, '2000/1', '1000/1',
        'invoice1_id', '2022-01-01T04:00:00.00Z'),
       ('product5_id', 'Machine agro-alimentaire', 1, '1000/1', '1000/1',
        'invoice2_id', '2022-01-01T04:00:00.00Z'),
       ('product6_id', 'Autres produits', 1, '1000/1', '1000/1',
        'invoice4_id', '2022-01-01T04:00:00.00Z');