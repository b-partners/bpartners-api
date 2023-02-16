insert into "account" (id, id_user, "name", iban, bic, available_balance, status)
values ('c15924bf-61f9-4381-8c9b-d34369bf91f7', 'joe_doe_id', 'Account_name', 'FR14 0123456789',
        'BP FR ' ||
        'PP CCT',
        '10000/1', 'OPENED');