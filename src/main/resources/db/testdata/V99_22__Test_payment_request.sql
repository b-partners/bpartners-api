--first payment_request
insert into payment_request (id, id_user, payer_name, payer_email, created_datetime, enable_status)
values ('da5009e8-502b-4ac2-a11d-4be45ccf30f3', 'joe_doe_id', 'Alec',
        'afylan0@jugem.jp', '2022-01-01T04:00:01.00Z', 'ENABLED');

--first Three payment requests
insert into payment_request (id, id_user, payer_name, payer_email, created_datetime, enable_status)
values ('6b6ffe80-1ca6-4a92-8bab-c21f817420fd', 'joe_doe_id', 'Kareem',
        'klaurent2p@hostgator.com', '2022-01-01T04:08:00.00Z', 'ENABLED');
insert into payment_request (id, id_user, payer_name, payer_email, created_datetime, enable_status)
values ('64fbed6a-e60b-4a17-965e-2a49578a2448', 'joe_doe_id', 'Yardley',
        'ylongson2q@pbs.org', '2022-01-01T04:09:00.00Z', 'ENABLED');
insert into payment_request (id, id_user, payer_name, payer_email, created_datetime, enable_status)
values ('67575c91-f275-4f68-b10a-0f30f96f7806', 'joe_doe_id', 'Chadd',
        'ctesto2r@blinklist.com', '2022-01-01T04:10:00.00Z', 'ENABLED');
insert into payment_request(id, id_user, id_invoice, payer_name, payer_email, amount,
                            label, reference, payment_due_date, payment_url, created_datetime, enable_status, "status")
values ('a1275c91-f275-4f68-b10a-0f30f96f7806', 'joe_doe_id',
        'invoice1_id', 'Luc Artisan', 'bpartners.artisans@gmail.com', '4400/1', 'Acompte 50%',
        'FAC2023ACT01', '2023-02-01', 'https://connect-v2-sbx.fintecture.com',
        '2023-01-01T00:00:00.00Z', 'ENABLED', 'UNPAID'),
       ('bab75c91-f275-4f68-b10a-0f30f96f7806', 'joe_doe_id',
        'invoice1_id', 'Luc Artisan', 'bpartners.artisans@gmail.com', '4400/1',
        'Reste 50%', 'FAC2023ACT02', '2023-02-01', 'https://connect-v2-sbx.fintecture.com', '2023-01-02T00:00:00.00Z', 'ENABLED',
        'PAID');