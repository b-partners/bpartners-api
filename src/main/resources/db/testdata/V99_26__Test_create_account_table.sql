insert into "account" (id, id_user, "name", iban, bic, available_balance, status)
values ('c15924bf-61f9-4381-8c9b-d34369bf91f7', 'joe_doe_id', 'Account_name', 'FR14 0123456789',
        'BP FR ' ||
        'PP CCT',
        '10000/1', 'OPENED'),
       ('account_to_validate_id', 'joe_doe_id', 'new_account_name', 'FR14 01234561089',
        'BP FR ' ||
        'PP CCT',
        '10000/1', 'VALIDATION_REQUIRED'),
       ('account_invalid_id', 'joe_doe_id', 'nvalid_account_name', 'FR14 01234581089',
        'BP FR ' ||
        'PP CCT',
        '10000/1', 'INVALID_CREDENTIALS');