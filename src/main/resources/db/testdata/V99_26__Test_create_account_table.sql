insert into "account" (id, id_user, "name", iban, bic, available_balance, status, external_id)
values ('beed1765-5c16-472a-b3f4-5c376ce5db58', 'joe_doe_id', 'Account_name', 'FR0123456789',
        'BIC_NOT_NULL', '10000/1', 'OPENED', 'beed1765-5c16-472a-b3f4-5c376ce5db58'),
       ('account_pro_id', 'bernard_doe_id', 'Account_pro', 'FR14 012345678',
        'BP FR PP CCT', '10000/1', 'VALIDATION_REQUIRED', 'account_pro_external_id'),
       ('jane_account_id', 'jane_doe_id', 'Jane account', 'IBAN1234',
        'BIC123', '0/1', 'OPENED', 'jane_account_id');