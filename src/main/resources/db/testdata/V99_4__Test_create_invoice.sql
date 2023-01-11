insert into "invoice"
(id, id_account, title, "ref", id_customer, sending_date, validity_date, to_pay_at, status, comment,
 "created_datetime", payment_url)
values ('invoice1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Outils pour plomberie',
        'BP001', 'customer1_id',
        '2022-09-01',
        '2022-10-03', '2022-10-01', 'CONFIRMED', null, '2022-01-01T01:00:00.00Z'
           , 'https://connect-v2-sbx.fintecture.com'),
       ('invoice2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'plomberie', 'BP002',
        'customer2_id',
        '2022-09-10',
        '2022-10-14', '2022-10-10', 'CONFIRMED', null, '2022-01-01T03:00:00.00Z',
        'https://connect-v2-sbx.fintecture.com');
insert into "invoice"
(id, id_account, title, "ref", id_customer, sending_date, validity_date, to_pay_at, status, comment,
 "created_datetime")
values ('invoice3_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction ', 'BP004',
        'customer1_id',
        '2022-10-12',
        '2022-10-03', '2022-11-10', 'DRAFT', null, '2022-01-01T02:00:00.00Z'),
       ('invoice4_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'achat', 'BP005',
        'customer1_id',
        '2022-10-12',
        '2022-10-03', '2022-11-13', 'PROPOSAL', null, '2022-01-01T04:00:00.00Z'),
       ('invoice5_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'achat', 'BP006',
        'customer1_id',
        '2022-10-12',
        '2022-10-03', '2022-11-13', 'PROPOSAL', null, '2022-01-01T05:00:00.00Z'),
       ('invoice6_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction', 'BP007',
        'customer1_id',
        '2022-10-12',
        '2022-11-12', '2022-11-10', 'DRAFT', null, '2022-01-01T06:00:00.00Z'),
       ('invoice7_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction', 'BP008',
        'customer1_id',
        '2022-10-12',
        '2022-10-03', '2022-11-10', 'PAID', null, '2022-01-01T07:00:00.00Z');