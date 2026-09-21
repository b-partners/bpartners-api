# Test live du traitement des impayes : subscriptionInvoice <-> Stripe

Objectif : reproduire, en sandbox / Stripe test, le rapprochement d'une facture
d'abonnement UNPAID cote interne avec la facture impayee correspondante cote
Stripe, et verifier que l'endpoint remonte le bon `paymentUrl`
(`hosted_invoice_url`) departage par montant ET par periode.

Ce que valide ce test :
- `paymentStatus=UNPAID` est bien derive quand la facture interne n'est pas PAID.
- `paymentUrl` = `hosted_invoice_url` d'une facture Stripe `OPEN`/`UNCOLLECTIBLE`
  (voir `getUnpaidStripeInvoices` : statuts OPEN + UNCOLLECTIBLE uniquement).
- Le departage par periode choisit la bonne facture Stripe quand plusieurs mois
  ont le meme montant.

## Fichiers

| Fichier | Role |
|---|---|
| `stripe_setup.sh` | Cree cote Stripe TEST une/des facture(s) OPEN avec `hosted_invoice_url`. |
| `stripe_fail_invoice.sh` | (Optionnel) fait passer une facture en echec de paiement / UNCOLLECTIBLE. |
| `seed_internal_invoices.sql` | Insere cote interne les factures CONFIRMED (== UNPAID) correspondantes. |
| `call_endpoint.sh` | Appelle l'endpoint et affiche `paymentStatus` + `paymentUrl` par mois. |

## Pre-requis a recuperer une seule fois

- `USER_ID` : id de l'utilisateur abonne a tester.
- Token bearer de CET utilisateur (l'endpoint est en self-auth).
- Cle Stripe TEST `sk_test_...` de l'env sandbox.
- `USER_TO_CREDIT_ID` : valeur de `subscription.user.credit.id`
  (SSM `/bpartners/<Env>/subscription/user/id`).
- `DATABASE_URL` de la base sandbox/preprod.
- Base URL de l'API sandbox/preprod (ex. `https://api.preprod.bpartners.app`).

> Tous les scripts ont leurs valeurs sensibles en `__TODO__` en tete de fichier :
> il faut les renseigner avant de lancer. Ne pas committer de valeurs reelles.

## Regle importante : les montants doivent etre egaux

Le rapprochement matche d'abord par MONTANT. `AMOUNT_CENTS` du seed interne
(TVA 0) DOIT etre strictement egal au `total` Stripe. Utilisez la meme valeur
dans `stripe_setup.sh` et dans la commande `psql` ci-dessous (exemple : `5880`).

## Etapes

### 1. Cote Stripe (mode test) : creer la/les facture(s) OPEN

Renseigner `USER_ID`, `STRIPE_SECRET_KEY`, `AMOUNT_CENTS`, `MONTHS` en tete de
`stripe_setup.sh` (laisser `CUSTOMER_ID` vide pour en creer un nouveau).

```
bash stripe_setup.sh
```

- Noter le `customer` (`cus_...`) et les `hosted_invoice_url` affiches par mois.
- Verifier : `STATUT=open`, `TOTAL == AMOUNT_CENTS`, et `PERIOD_END` aligne sur
  le mois (necessaire pour valider le departage par periode en live).

### 2. (Optionnel) Simuler l'echec de paiement / l'irrecouvrable

Pour reproduire un impaye issu d'un echec plutot qu'une simple facture en attente,
renseigner `KEY` (sk_test) et `INVOICE_ID` (`in_...` de l'etape 1) en tete de
`stripe_fail_invoice.sh`, puis :

```
bash stripe_fail_invoice.sh
```

- `2a` (par defaut) tente un paiement avec une carte de test refusee
  (`pm_card_chargeDeclined`) : la facture reste OPEN / en echec.
- `2b` (commentee) marque la facture `uncollectible`.

Dans les deux cas la facture reste dans le pool `OPEN + UNCOLLECTIBLE`, donc son
`hosted_invoice_url` reste eligible au rapprochement.

### 3. Cote interne : inserer les factures CONFIRMED correspondantes

```
psql "$DATABASE_URL" \
  -v USER_ID="'<USER_ID>'" \
  -v USER_TO_CREDIT_ID="'<USER_TO_CREDIT_ID>'" \
  -v STRIPE_CUSTOMER_ID="'<cus_... de l'etape 1>'" \
  -v AMOUNT_CENTS=5880 \
  -v M1=2025-07-01 -v M2=2025-08-01 \
  -f seed_internal_invoices.sql
```

- Un seul mois : mettre `M2` = `M1` (la 2e ligne est dedupliquee par ON CONFLICT).
- Passer `M1`/`M2` SANS quotes internes (la forme `:'M1'::date` s'en charge).
- Ce script rattache aussi le `cus_...` Stripe a `user.user_subscription_e2_id`
  (indispensable au lookup), et cree un customer interne portant l'email de l'abonne.

### 4. Appeler l'endpoint et verifier le rapprochement

Renseigner `USER_ID`, `BASE_URL`, `BEARER`, `MONTHS` en tete de `call_endpoint.sh`.

```
bash call_endpoint.sh
```

Attendu, pour chaque mois : `paymentStatus=UNPAID` et `paymentUrl` = l'URL Stripe
du meme mois (departage par periode) et du meme montant.

## Nettoyage (sandbox)

Cote interne (remplacer `<USER_TO_CREDIT_ID>` par la valeur utilisee au seed) :

```
psql "$DATABASE_URL" -c "delete from invoice_product where id_invoice like 'sandbox_sub_inv_%';
delete from invoice where id like 'sandbox_sub_inv_%';
delete from customer where id = 'sandbox_cust_<USER_TO_CREDIT_ID>';"
```

Cote Stripe test : voider les factures OPEN creees (dashboard test ou API
`POST /v1/invoices/<in_...>/void`).

## Notes

- `AMOUNT_CENTS` interne (TVA 0) doit egaler le `total` Stripe pour matcher.
- Le departage par periode s'appuie sur `period_end` de la facture Stripe (sinon
  `created`). Si `stripe_setup.sh` affiche un `PERIOD_END` aligne sur le mois, le
  departage est testable en live ; sinon il reste couvert par les tests unitaires/IT.
- Rien n'est ecrit cote paiement reel : les factures Stripe sont en `send_invoice`
  (OPEN, en attente), ce qui suffit a `getUnpaidStripeInvoices` (OPEN + UNCOLLECTIBLE).
