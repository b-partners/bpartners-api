#!/usr/bin/env bash
#
# Cree, en mode TEST Stripe, une ou plusieurs factures OPEN (impayees) avec
# hosted_invoice_url, pour simuler le "echoue sur Stripe" du cote passerelle.
#
# Prerequis : python3 (calcul des dates), curl. Aucune dependance jq.
#
# ============================ CONFIG A RENSEIGNER ============================
USER_ID="__TODO__"                 # id de l'utilisateur abonne (informatif ici)
STRIPE_SECRET_KEY="__TODO_sk_test__"

# Laisser vide pour CREER un nouveau customer de test ; sinon mettre le customer
# Stripe TEST deja rattache a l'utilisateur (== user.user_subscription_e2_id).
CUSTOMER_ID="__TODO_cus__"
CUSTOMER_EMAIL="__TODO__"   # utilise seulement si on cree le customer

AMOUNT_CENTS=5880                          # 1200 = 12,00 EUR (doit == total interne)
CURRENCY="eur"

# Mois couverts (format YYYY-MM). Deux mois => teste le departage par periode.
# Un seul mois suffit pour valider le fonctionnement de base.
MONTHS=("2025-07" "2025-08")
# ===========================================================================

set -euo pipefail
API="https://api.stripe.com/v1"
auth=(-u "${STRIPE_SECRET_KEY}:")

jget() { python3 -c 'import sys,json;print(json.load(sys.stdin).get(sys.argv[1],""))' "$1"; }
month_start() { python3 -c 'import datetime,sys;y,m=map(int,sys.argv[1].split("-"));print(int(datetime.datetime(y,m,1).timestamp()))' "$1"; }
month_end()   { python3 -c 'import datetime,calendar,sys;y,m=map(int,sys.argv[1].split("-"));d=calendar.monthrange(y,m)[1];print(int(datetime.datetime(y,m,d,23,59,59).timestamp()))' "$1"; }
epoch2date()  { python3 -c 'import datetime,sys;print(datetime.datetime.fromtimestamp(int(sys.argv[1])).date() if sys.argv[1] else "")' "$1"; }

if [[ -z "${CUSTOMER_ID}" ]]; then
  echo ">> Creation d'un customer de test..."
  CUSTOMER_ID=$(curl -s "${API}/customers" "${auth[@]}" \
    -d description="sandbox test ${USER_ID}" \
    -d email="${CUSTOMER_EMAIL}" | jget id)
  echo "   customer cree: ${CUSTOMER_ID}"
  echo "   >>> Renseigner cet id dans user.user_subscription_e2_id (voir seed_internal_invoices.sql)"
else
  echo ">> Customer existant: ${CUSTOMER_ID}"
fi

printf '\n%-9s | %-27s | %-8s | %-7s | %-10s | %s\n' "MOIS" "INVOICE_ID" "STATUT" "TOTAL" "PERIOD_END" "HOSTED_URL"
printf -- '-%.0s' {1..140}; echo

for M in "${MONTHS[@]}"; do
  ps=$(month_start "$M"); pe=$(month_end "$M")

  curl -s "${API}/invoiceitems" "${auth[@]}" \
    -d customer="${CUSTOMER_ID}" \
    -d amount="${AMOUNT_CENTS}" \
    -d currency="${CURRENCY}" \
    -d "period[start]=${ps}" \
    -d "period[end]=${pe}" \
    -d description="Abonnement (test) periode ${M}" >/dev/null

  inv=$(curl -s "${API}/invoices" "${auth[@]}" \
    -d customer="${CUSTOMER_ID}" \
    -d collection_method=send_invoice \
    -d days_until_due=30 \
    -d auto_advance=false)
  inv_id=$(echo "$inv" | jget id)

  fin=$(curl -s "${API}/invoices/${inv_id}/finalize" "${auth[@]}")
  status=$(echo "$fin" | jget status)
  total=$(echo "$fin" | jget total)
  purl=$(echo "$fin" | jget hosted_invoice_url)
  pend=$(echo "$fin" | jget period_end)

  printf '%-9s | %-27s | %-8s | %-7s | %-10s | %s\n' \
    "$M" "$inv_id" "$status" "$total" "$(epoch2date "$pend")" "$purl"
done

echo
echo ">> Verifier ci-dessus : STATUT=open, TOTAL=${AMOUNT_CENTS}, et PERIOD_END aligne sur le mois."
echo ">> Si PERIOD_END ne reflete pas le mois (== date du jour), le departage par periode"
echo "   ne pourra pas etre valide en live (couvert par les tests unitaires/IT)."
