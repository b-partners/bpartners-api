with cibles(user_id) as (values ('e0981997-2e0c-4e27-946e-8ec53271e364'),
                                ('bab8463a-beb6-4a8c-b5ac-2890d9a349c7'),
                                ('c4eda4a8-2044-4b42-9321-6ebe8164872e'),
                                ('293720a5-1994-4643-904c-dd7a41b58366'),
                                ('f55999c4-deb5-4fa8-9755-a1fb9230f04f'),
                                ('6d394379-585e-4471-b42e-213dc7624a55'),
                                ('8160db00-4f50-4c71-9643-5774e7cf2f53'),
                                ('153dc422-1303-4c5c-b730-02be0779c770'),
                                ('1dbbbefc-b447-4e61-accc-51699b87bbca'),
                                ('1cadb888-cd29-4388-9c0a-e81cf12a9367'),
                                ('5c5f431b-7d46-4f40-9b6f-21fc01c4e983'),
                                ('d496a5bb-3327-4086-8d9d-963d098799ce'),
                                ('2d56d559-0a3c-4824-b410-70b5d8ad0199'),
                                ('76be4f2d-75f6-48c3-a767-e6e26b28c6bb'))
select u.email,
       (select count(*) from user_subscription_commitment k
         where k.user_id = u.id and k.creation_datetime >= current_date)        as engagement_cree_aujourdhui,
       (select count(*) from subscription_payment sp
         where sp.user_id = u.id and sp.creation_datetime >= current_date)      as paiement_cree_aujourdhui,
       (select string_agg(i."ref", ', ' order by i."ref")
        from subscription_payment sp
                 join "invoice" i on i.id = sp.invoice_id
        where sp.user_id = u.id
          and i.created_datetime >= current_date)                               as factures_creees_aujourdhui,
       (select count(*) from subscription_payment sp
         where sp.user_id = u.id
           and sp.payment_datetime >= timestamp '2026-10-01 00:00:00'
           and sp.invoice_id is null)                                           as paiement_oct_sans_facture,
       case
           when (select count(*) from subscription_payment sp
                  where sp.user_id = u.id and sp.creation_datetime >= current_date) = 0
               then 'NON TRAITE'
           when (select count(*) from subscription_payment sp
                  where sp.user_id = u.id
                    and sp.payment_datetime >= timestamp '2026-10-01 00:00:00'
                    and sp.invoice_id is null) > 0
               then 'TRAITE A MOITIE'
           else 'TRAITE'
           end                                                                  as etat
from cibles c
         join "user" u on u.id = c.user_id
order by etat, u.email;
