-- Insere le cote INTERNE : factures d'abonnement CONFIRMED (== UNPAID) pour un
-- utilisateur abonne, rattachees a un customer dont l'email = email de l'abonne.
--
-- A lancer avec psql, en renseignant les variables ci-dessous, ex :
--   psql "$DATABASE_URL" \
--     -v USER_ID="'the_subscriber_user_id'" \
--     -v USER_TO_CREDIT_ID="'the_user_to_credit_id'" \
--     -v STRIPE_CUSTOMER_ID="'cus_xxx_from_stripe_setup'" \
--     -v AMOUNT_CENTS=1200 \
--     -v M1=2025-07-01 \
--     -v M2=2025-08-01 \
--     -f seed_internal_invoices.sql
--
-- Note : passer M1/M2 SANS quotes internes (la forme :'M1'::date les quote).
--
-- USER_TO_CREDIT_ID = valeur de subscription.user.credit.id
--   (SSM /bpartners/<Env>/subscription/user/id).
-- STRIPE_CUSTOMER_ID = customer TEST affiche par stripe_setup.sh.
-- Pour ne tester qu'un seul mois, mettre M2 identique a M1 (la 2e ligne
--   est dedupliquee par ON CONFLICT DO NOTHING).

\set ON_ERROR_STOP on

-- 1) Rattacher le customer Stripe TEST a l'utilisateur (indispensable au lookup).
update "user"
set user_subscription_e2_id = :STRIPE_CUSTOMER_ID
where id = :USER_TO_CREDIT_ID;

-- 2) Customer interne (sous user_to_credit) portant l'email de l'abonne.
insert into "customer" (id, id_user, first_name, last_name, email, phone, customer_type)
select 'sandbox_cust_' || :USER_TO_CREDIT_ID,
       :USER_ID,
       coalesce(u.first_name, 'Sandbox'),
       coalesce(u.last_name, 'Subscriber'),
       u.email,
       coalesce(u.phone_number, '+261340000000'),
       'PROFESSIONAL'
from "user" u
where u.id = :USER_TO_CREDIT_ID
on conflict (id) do update set email = excluded.email;

-- 3) Factures d'abonnement (une par mois), statut CONFIRMED.
insert into "invoice"
  (id, id_user, title, "ref", id_customer, customer_email, sending_date, validity_date,
   to_pay_at, status, archive_status, created_datetime, payment_type)
select 'sandbox_sub_inv_' || to_char(mo.m, 'YYYYMM'),
       :USER_ID,
       'Facture pour la période de ' || to_char(mo.m, 'DD/MM/YYYY')
         || ' au ' || to_char((mo.m + interval '1 month - 1 day')::date, 'DD/MM/YYYY'),
       'SANDBOX-' || to_char(mo.m, 'YYYYMM'),
       'sandbox_cust_' || :USER_TO_CREDIT_ID,
       (select email from "user" where id = :USER_TO_CREDIT_ID),
       (mo.m + interval '1 month - 1 day')::date,
       (mo.m + interval '2 month - 1 day')::date,
       (mo.m + interval '1 month')::date,
       'CONFIRMED', 'ENABLED', now(), 'CASH'
from (values (:'M1'::date), (:'M2'::date)) as mo(m)
on conflict (id) do nothing;

-- 4) Ligne produit : total en centimes = AMOUNT_CENTS (TVA 0).
insert into "invoice_product"
  (id, id_invoice, description, quantity, unit_price, vat_percent, status)
select 'sandbox_sub_inv_' || to_char(mo.m, 'YYYYMM') || '_p1',
       'sandbox_sub_inv_' || to_char(mo.m, 'YYYYMM'),
       'Abonnement (sandbox) ' || to_char(mo.m, 'MM/YYYY'),
       1, (:AMOUNT_CENTS)::text, '0', 'ENABLED'
from (values (:'M1'::date), (:'M2'::date)) as mo(m)
on conflict (id) do nothing;

-- 5) Controle.
select i.id, i.title, i.status, c.email as customer_email
from "invoice" i join "customer" c on c.id = i.id_customer
where i.id like 'sandbox_sub_inv_%'
order by i.sending_date;
