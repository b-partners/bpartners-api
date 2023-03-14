insert into transaction
(id, id_swan, id_account, amount, currency, label, reference, side, status, payment_date_time)
values ('transaction1_id', 'bosci_f224704f2555a42303e302ffb8e69eef',
        'beed1765-5c16-472a-b3f4-5c376ce5db58', '50000/1', 'EUR', 'Création de site vitrine',
        'REF_001',
        'CREDIT_SIDE', 'PENDING',
        '2022-08-26T06:33:50.595Z'),
       ('transaction2_id', 'bosci_0fe167566b234808a44aae415f057b6c',
        'beed1765-5c16-472a-b3f4-5c376ce5db58', '50000/1', 'EUR', 'Premier virement', 'JOE-001',
        'CREDIT_SIDE', 'BOOKED',
        '2022-08-24T03:39:33.315Z');