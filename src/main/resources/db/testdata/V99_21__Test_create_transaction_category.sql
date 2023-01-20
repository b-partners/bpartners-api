insert into "transaction_category"
(id, id_account, id_transaction_category_tmpl, "type", vat, id_transaction, created_datetime)
values ('transaction_category1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58',
        '65a891b8-43e3-402a-9b36-073a0fed680e', null,
        null,
        'transaction2_id', '2022-01-01T01:00:00.00Z'),
       ('transaction_category2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58',
        'aa66239c-2be3-4c04-b260-49b207951ecf', 'Sponsoring',
        '0/1',
        'transaction2_id', '2022-01-01T01:00:00.00Z');