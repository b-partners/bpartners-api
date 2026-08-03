insert into "user"
(id, phone_number, email, status, logo_file_id, first_name,
 last_name, old_s3_id_account, preferred_account_id, user_subscription_e2_id)
values ('joe_doe_id', '+261340465338', 'joe@email.com',
        'ENABLED',
        'logo.jpeg', 'Joe', 'Doe', 'old_s3_key',
        'beed1765-5c16-472a-b3f4-5c376ce5db58', 'cus_REyMbSpHZjHftA'),
       ('jane_doe_id', '+261341122334', 'jane@email.com', 'ENABLED',
        'logo.jpeg', 'Jane', 'Doe', null, null, 'cus_LyAuPSpHZjHftA'),
       ('bernard_doe_id', '+261342463616', 'bernard@email.com',
        'ENABLED',
        'logo.jpeg', 'Bernard', 'Doe', null, null, 'cus_RFLvHEh3ileMmV');