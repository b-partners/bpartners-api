insert into transaction (id, id_swan, id_account, amount, currency, label, reference, side, status, payment_date_time, "type")
values ('transaction1_id', 'bosci_f224704f2555a42303e302ffb8e69eef',
        'beed1765-5c16-472a-b3f4-5c376ce5db58', 50000, 'EUR', 'Premier virement', 'JOE-001', 'CREDIT_SIDE','PENDING', '2022-01-01T03:00:00.00Z', 'INCOME'),
       ('transaction2_id', 'bosci_0fe167566b234808a44aae415f057b6c',
        'beed1765-5c16-472a-b3f4-5c376ce5db58', 10000, 'EUR', 'Deuxieme virement', 'JOE_002', 'CREDIT_SIDE', 'PENDING', '2022-01-02T03:00:00.00Z', 'INCOME');