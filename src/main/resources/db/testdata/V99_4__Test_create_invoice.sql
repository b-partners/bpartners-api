insert into "invoice"
(id, id_account, title, "ref", sending_date, to_pay_at, status, comment)
values ('invoice1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture tableau',
        'BP001',
        '2022-09-01',
        '2022-10-01', 'CONFIRMED', 'Tableau de Madagascar'),
       ('invoice2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'plomberie', 'BP002',
        '2022-09-10',
        '2022-10-10', 'CONFIRMED', null),
       ('invoice3_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction ', 'BP004',
        '2022-10-12',
        '2022-11-10', 'DRAFT', null),
       ('invoice4_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'achat', 'BP005',
        '2022-10-12',
        '2022-11-13', 'PROPOSAL', null),
       ('invoice5_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'achat', 'BP006',
        '2022-10-12',
        '2022-11-13', 'PROPOSAL', null),
       ('invoice6_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction', 'BP007',
        '2022-10-12',
        '2022-11-10', 'DRAFT', null),
       ('invoice7_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Facture ' ||
                                                               'transaction', 'BP008',
        '2022-10-12',
        '2022-11-10', 'PAID', null);