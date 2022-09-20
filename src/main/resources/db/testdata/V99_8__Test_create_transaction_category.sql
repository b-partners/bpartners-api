insert into "transaction_category"
(id, id_account, id_transaction_category_tmpl, "type", vat, id_transaction, created_datetime)
values ('transaction_category1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'tc_tmpl1_id', null,
        null,
        'bosci_0fe167566b234808a44aae415f057b6c', '2022-01-01T01:00:00.00Z'),
       ('transaction_category2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'tc_tmpl2_id', null,
        null,
        'bosci_28cb4daf35d3ab24cb775dcdefc8fdab', '2022-01-01T01:00:00.00Z'),
       ('transaction_category3_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'tc_tmpl2_id', null,
        null,
        'bosci_28cb4daf35d3ab24cb775dcdefc8fdab', '2022-01-01T01:10:00.00Z'),
       ('transaction_category4_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', null, 'Recette '
           ||
                                                                                  'personnalisée TVA 1%',
        100,
        'bosci_28cb4daf35d3ab24cb775dcdefc8fdab', '2022-01-01T01:20:00.00Z');