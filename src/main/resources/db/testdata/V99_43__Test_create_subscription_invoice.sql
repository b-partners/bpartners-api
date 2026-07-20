
insert into "user"
(id, phone_number, email, status, logo_file_id, first_name, last_name)
values ('user_to_credit_id', '+261340000001', 'billing@bpartners.com', 'ENABLED', 'logo.jpeg',
        'BPartners', 'Billing'),
       ('subscriber_id', '+261340000002', 'subscriber@email.com', 'ENABLED', 'logo.jpeg',
        'Sub', 'Scriber'),
       ('stripe_subscriber_id', '+261340000003', 'stripe.subscriber@email.com', 'ENABLED',
        'logo.jpeg', 'Stripe', 'Scriber');

insert into "customer"
(id, id_user, first_name, last_name, email, phone, customer_type)
values ('subscription_customer_id', 'user_to_credit_id', 'Sub', 'Scriber',
        'subscriber@email.com', '+261340000002', 'PROFESSIONAL'),
       ('stripe_subscription_customer_id', 'user_to_credit_id', 'Stripe', 'Scriber',
        'billing.stripe@email.com', '+261340000003', 'PROFESSIONAL'),
       ('other_subscription_customer_id', 'user_to_credit_id', 'Other', 'Scriber',
        'other.subscriber@email.com', '+261340000004', 'PROFESSIONAL');

insert into "user_stripe_customer_email_correspondence"
(id, user_id, stripe_customer_id, email)
values ('stripe_correspondence_id', 'stripe_subscriber_id', 'cus_subscription_test',
        'billing.stripe@email.com');

insert into "invoice"
(id, id_user, title, "ref", id_customer, sending_date, validity_date, to_pay_at, status,
 archive_status, "created_datetime", payment_type)
values
    ('subscription_invoice_march_id', 'user_to_credit_id',
     'Facture pour la période de 01/03/2024 au 31/03/2024', 'BPS001', 'subscription_customer_id',
     '2024-03-31', '2024-04-30', '2024-04-05', 'CONFIRMED', 'ENABLED',
     '2024-03-31T10:00:00.00Z', 'CASH'),
    ('subscription_invoice_february_id', 'user_to_credit_id',
     'Facture pour la période de 01/02/2024 au 29/02/2024', 'BPS002', 'subscription_customer_id',
     '2024-02-29', '2024-03-30', '2024-03-05', 'CONFIRMED', 'ENABLED',
     '2024-02-29T10:00:00.00Z', 'CASH'),
    ('subscription_invoice_other_customer_id', 'user_to_credit_id',
     'Facture pour la période de 01/03/2024 au 31/03/2024', 'BPS003',
     'other_subscription_customer_id', '2024-03-31', '2024-04-30', '2024-04-05', 'CONFIRMED',
     'ENABLED', '2024-03-31T11:00:00.00Z', 'CASH'),
    ('subscription_invoice_draft_id', 'user_to_credit_id',
     'Facture pour la période de 01/03/2024 au 31/03/2024', 'BPS004', 'subscription_customer_id',
     '2024-03-15', '2024-04-14', '2024-04-05', 'DRAFT', 'ENABLED',
     '2024-03-15T10:00:00.00Z', 'CASH'),
    ('subscription_invoice_archived_id', 'user_to_credit_id',
     'Facture pour la période de 01/03/2024 au 31/03/2024', 'BPS005', 'subscription_customer_id',
     '2024-03-20', '2024-04-19', '2024-04-05', 'CONFIRMED', 'DISABLED',
     '2024-03-20T10:00:00.00Z', 'CASH'),
    ('stripe_subscription_invoice_march_id', 'user_to_credit_id',
     'Facture pour la période de 01/03/2024 au 31/03/2024', 'BPS006',
     'stripe_subscription_customer_id', '2024-03-31', '2024-04-30', '2024-04-05', 'PAID',
     'ENABLED', '2024-03-31T12:00:00.00Z', 'CASH');
