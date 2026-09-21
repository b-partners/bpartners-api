KEY="__TODO_sk_test__"
INVOICE_ID="__TODO_in__"

## 1) Finaliser : draft -> open (+ hosted_invoice_url)
#curl -s "https://api.stripe.com/v1/invoices/${INVOICE_ID}/finalize" -u "${KEY}:" \
#  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("status"), d.get("hosted_invoice_url"))'

# 2a) Provoquer un ECHEC de paiement (carte de test refusee)
curl -s "https://api.stripe.com/v1/invoices/${INVOICE_ID}/pay" -u "${KEY}:" \
  -d payment_method=pm_card_chargeDeclined \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("status"), (d.get("last_finalization_error") or {}).get("message"))'

# 2b) OU marquer irrecouvrable
# curl -s "https://api.stripe.com/v1/invoices/${INVOICE_ID}/mark_uncollectible" -u "${KEY}:"