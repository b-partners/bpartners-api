insert into "product"
(id, id_account, description, quantity, unit_price, vat_percent, id_invoice_product,
 created_datetime)
values ('product1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Tableau malgache', 1, '1000/1', '2000/1',
        'invoice_product1_id', '2022-01-01T01:00:00.00Z'),
       ('product2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Tableau baobab', 2, '2000/1', '1000/1',
        'invoice_product1_id', '2022-01-01T02:00:00.00Z'),
       ('product3_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Tableau baobab', 3, '2000/1', '1000/1',
        'invoice_product2_id', '2022-01-01T03:00:00.00Z'),
       ('product4_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Tableau malgache', 1, '2000/1', '1000/1',
        'invoice_product2_id', '2022-01-01T04:00:00.00Z'),
       ('product5_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Mon tableau', 1, '1000/1', '1000/1',
        'invoice_product3_id', '2022-01-01T04:00:00.00Z'),
       ('product6_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Mon tableau', 1, '1000/1', '1000/1',
        'invoice_product4_id', '2022-01-01T04:00:00.00Z');