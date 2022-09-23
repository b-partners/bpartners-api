insert into "invoice"
(id, id_account, id_customer, title, "ref", vat, sending_date, to_pay_at, status)
values ('invoice1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'customer1_id', 'Facture tableau',
        'BP001',
        1000,
        '2022-09-01',
        '2022-10-01', 'CONFIRMED'),
       ('invoice2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'customer2_id', 'Facture ' ||
                                                                               'plomberie', 'BP002',
        1000,
        '2022-09-10',
        '2022-10-10', 'CONFIRMED');