insert into "account" (id, id_user, "name", iban, bic, available_balance)
values ('beed1765-5c16-472a-b3f4-5c376ce5db58', 'joe_doe_id', 'Account_name', 'FR0123456789',
        'BIC_NOT_NULL', '10000/1'),
       ('other_joe_account_id', 'joe_doe_id', 'Other joe account', 'Other iban',
        'Other bic', '0/1'),
       ('account_pro_id', 'bernard_doe_id', 'Account_pro', 'FR14 012345678',
        'BP FR PP CCT', '10000/1'),
       ('jane_account_id', 'jane_doe_id', 'Jane account', 'IBAN1234',
        'BIC123', '0/1');