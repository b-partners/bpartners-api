--first payment_request
insert into payment_request (id, account_id, payer_name, payer_email, created_datetime)
values ('da5009e8-502b-4ac2-a11d-4be45ccf30f3', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Alec',
        'afylan0@jugem.jp', '2022-01-01T04:00:01.00Z');

--first Three payment requests
insert into payment_request (id, account_id, payer_name, payer_email, created_datetime)
values ('6b6ffe80-1ca6-4a92-8bab-c21f817420fd', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Kareem',
        'klaurent2p@hostgator.com', '2022-01-01T04:08:00.00Z');
insert into payment_request (id, account_id, payer_name, payer_email, created_datetime)
values ('64fbed6a-e60b-4a17-965e-2a49578a2448', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Yardley',
        'ylongson2q@pbs.org', '2022-01-01T04:09:00.00Z');
insert into payment_request (id, account_id, payer_name, payer_email, created_datetime)
values ('67575c91-f275-4f68-b10a-0f30f96f7806', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Chadd',
        'ctesto2r@blinklist.com', '2022-01-01T04:10:00.00Z');
insert into payment_request(id, account_id, id_invoice, payer_name, payer_email, amount,
                            label, reference, payment_due_date, payment_url, created_datetime, "status")
values ('a1275c91-f275-4f68-b10a-0f30f96f7806', 'beed1765-5c16-472a-b3f4-5c376ce5db58',
        'invoice1_id', 'Luc Artisan', 'bpartners.artisans@gmail.com', '4400/1', 'Acompte 50%',
        'FAC2023ACT01', '2023-02-01', 'https://connect-v2-sbx.fintecture.com',
        '2023-01-01T00:00:00.00Z', 'UNPAID'),
       ('bab75c91-f275-4f68-b10a-0f30f96f7806', 'beed1765-5c16-472a-b3f4-5c376ce5db58',
        'invoice1_id', 'Luc Artisan', 'bpartners.artisans@gmail.com', '4400/1',
        'Reste 50%', 'FAC2023ACT02', '2023-02-01', 'https://connect-v2-sbx.fintecture.com', '2023-01-02T00:00:00.00Z',
        'PAID');