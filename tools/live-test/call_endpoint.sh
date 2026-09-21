#!/usr/bin/env bash
#
# Appelle l'endpoint et affiche, par mois, le paymentStatus et le paymentUrl
# rapproches depuis Stripe. Auth = bearer de l'utilisateur lui-meme (self).
#
# ============================ CONFIG A RENSEIGNER ============================
USER_ID="__TODO__"
BASE_URL="https://api.preprod.bpartners.app"      # base de l'env sandbox/preprod
BEARER="__TODO__"             # Authorization: Bearer <...>
MONTHS=("2025-08")
# ===========================================================================

set -euo pipefail

for M in "${MONTHS[@]}"; do
  echo "===== yearMonth=${M} ====="
  curl -s "${BASE_URL}/users/${USER_ID}/subscriptionInvoices?yearMonth=${M}&paymentStatuses=UNPAID" \
    -H "Authorization: Bearer ${BEARER}" \
  | python3 -c '
import sys, json
data = json.load(sys.stdin)
if not isinstance(data, list):
    print(json.dumps(data, indent=2, ensure_ascii=False)); sys.exit(0)
for si in data:
    inv = si.get("invoice") or {}
    print(f"  invoice={inv.get('"'"'id'"'"')}  title={inv.get('"'"'title'"'"')}")
    print(f"     paymentStatus={si.get('"'"'paymentStatus'"'"')}  paymentUrl={si.get('"'"'paymentUrl'"'"')}")
'
  echo
done
