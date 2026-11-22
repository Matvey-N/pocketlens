#!/usr/bin/env bash
# End-to-end API walkthrough (see docs/guides/tutorial-import-and-ask.md). Requires curl and jq.
# Usage: BASE=http://localhost:8081/api/v1 ./docs/examples/walkthrough.sh
set -euo pipefail
BASE=${BASE:-http://localhost:8081/api/v1}
DIR=$(cd "$(dirname "$0")" && pwd)
EMAIL="demo-$(date +%s)@example.com"
JAR=$(mktemp)

echo "1. Register $EMAIL"
VERSION=$(curl -fsS "$BASE/privacy-notice" | jq -r .version)
TOKEN=$(curl -fsS -c "$JAR" -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Spring2026secure\",\"displayName\":\"Demo\",\"acceptPrivacyNotice\":true,\"privacyNoticeVersion\":\"$VERSION\"}" \
  | jq -r .accessToken)
AUTH="Authorization: Bearer $TOKEN"

echo "2. Import sample statement (twice: the second import finds only duplicates)"
curl -fsS -X POST "$BASE/imports?format=generic" -H "$AUTH" -F "file=@$DIR/sample-generic.csv" | jq '{imported, duplicates, needsReview}'
curl -fsS -X POST "$BASE/imports?format=generic" -H "$AUTH" -F "file=@$DIR/sample-generic.csv" | jq '{imported, duplicates}'

echo "3. Transactions that need review"
curl -fsS "$BASE/transactions?needsReview=true" -H "$AUTH" | jq '.items[] | {merchant, amount}'

echo "4. Budget for groceries and dashboard"
GROCERIES=$(curl -fsS "$BASE/categories" -H "$AUTH" | jq -r '.[] | select(.systemKey=="groceries") | .id')
curl -fsS -X POST "$BASE/budgets" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$GROCERIES\",\"monthlyLimit\":50,\"currency\":\"EUR\"}" > /dev/null
curl -fsS "$BASE/dashboard?month=2026-11" -H "$AUTH" | jq '{currencies: [.currencies[] | {currency, expenses, income}], budgets: [.budgets[] | {categoryName, health}]}'

echo "5. Subscriptions"
curl -fsS "$BASE/subscriptions" -H "$AUTH" | jq '.[] | {merchant, cadence, priceIncreased}'

echo "6. Ask the assistant (needs AI_API_KEY on the server)"
CONV=$(curl -fsS -X POST "$BASE/conversations" -H "$AUTH" | jq -r .id)
if ANSWER=$(curl -fsS -X POST "$BASE/conversations/$CONV/messages" -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"content":"How much did I spend on groceries in November 2026?"}'); then
  echo "$ANSWER" | jq '.answer | {content, interpretation, sourceTransactionCount}'
  MSG=$(echo "$ANSWER" | jq -r .answer.id)
  curl -fsS "$BASE/conversations/$CONV/messages/$MSG/transactions" -H "$AUTH" | jq .totals
else
  echo "Assistant unavailable (no AI key configured)"
fi
rm -f "$JAR"
