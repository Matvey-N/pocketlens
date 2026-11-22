# Tutorial: import a statement, correct a category, ask the assistant

A complete integration workflow. Prerequisite: the steps of [Getting started](getting-started.md)
(`BASE`, `TOKEN` and `cookies.txt` are set). A runnable version is in [`examples/walkthrough.sh`](../examples/walkthrough.sh).

## Step 1 — Choose a format

```bash
curl -s $BASE/imports/formats -H "Authorization: Bearer $TOKEN" | jq '.[].id'
# "eu-debit-credit" "generic" "revolut" "wise"
```

Unknown bank? Upload the file to `POST /imports/preview` to see its headers, then save a mapping with
`POST /import-profiles` and use its `id` as `profileId`.

## Step 2 — Import

```bash
curl -s -X POST "$BASE/imports?format=generic" -H "Authorization: Bearer $TOKEN" \
  -F file=@docs/examples/sample-generic.csv | jq
```

```json
{ "rowsRead": 12, "imported": 12, "duplicates": 0, "categorizedByRules": 9, "categorizedByAi": 2,
  "needsReview": 1, "aiAvailable": true, … }
```

Run the same command again: `imported` is `0` and `duplicates` is `12` — re-imports never double-count.

If the file does not match the format you get `422 IMPORT_COLUMNS_MISSING` with `missingColumns` and
`foundColumns`; nothing is imported.

## Step 3 — Review uncertain transactions

```bash
curl -s "$BASE/transactions?needsReview=true" -H "Authorization: Bearer $TOKEN" | jq '.items[] | {id, merchant}'
CATEGORY=$(curl -s $BASE/categories -H "Authorization: Bearer $TOKEN" | jq -r '.[] | select(.name=="Eating out") | .id')

curl -s -X PATCH $BASE/transactions/<transaction-id> -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{\"categoryId\":\"$CATEGORY\",\"applyToMerchant\":true}" | jq
```

`applyToMerchant: true` creates a rule — future imports of this merchant are categorised without AI
(`GET /rules` lists your rules).

## Step 4 — Set a budget and read the dashboard

```bash
GROCERIES=$(curl -s $BASE/categories -H "Authorization: Bearer $TOKEN" | jq -r '.[] | select(.systemKey=="groceries") | .id')
curl -s -X POST $BASE/budgets -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"categoryId\":\"$GROCERIES\",\"monthlyLimit\":300,\"currency\":\"EUR\"}"
curl -s "$BASE/dashboard?month=2026-11" -H "Authorization: Bearer $TOKEN" | jq '.budgets'
```

A budget with `health: "AT_RISK"` carries `projectedOverrunDate` — the early warning.

## Step 5 — Ask the assistant

```bash
CONV=$(curl -s -X POST $BASE/conversations -H "Authorization: Bearer $TOKEN" | jq -r .id)
curl -s -X POST $BASE/conversations/$CONV/messages -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"content":"How much did I spend on groceries in November 2026?"}' | tee answer.json | jq .answer
```

The answer contains `interpretation` (how the question was understood) and `sourceTransactionCount`.

## Step 6 — Verify the figure

```bash
MSG=$(jq -r .answer.id answer.json)
curl -s $BASE/conversations/$CONV/messages/$MSG/transactions -H "Authorization: Bearer $TOKEN" | jq .totals
```

The total equals the figure in the answer: the model chose the query, the database did the arithmetic.

### Errors to handle

| Status | Code | What to do |
|---|---|---|
| 422 | `PROMPT_REJECTED` | Rephrase; the question looked like a prompt-injection attempt |
| 429 | `RATE_LIMITED` | Wait `Retry-After` seconds (limit 20 questions/minute) |
| 503 | `AI_UNAVAILABLE` | Retry later; the question is kept in the conversation |
