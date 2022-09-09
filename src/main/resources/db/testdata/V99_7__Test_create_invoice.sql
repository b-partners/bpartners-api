insert into "invoice"
(id, id_account, id_customer, "ref", vat, invoice_date, to_pay_at, percentage_reduction,
 amount_reduction,
 status)
values ('invoice1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'customer1_id', 'BP001', 19,
        '2022-09-01',
        '2022-10-01', null, null, 'CONFIRMED'),
       ('invoice2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'customer2_id', 'BP002', 20,
        '2022-09-10',
        '2022-10-10', null, 10, 'CONFIRMED');