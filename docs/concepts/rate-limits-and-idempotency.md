# Rate limits and idempotency

## Rate limits

| Endpoint | Limit | Response when exceeded |
|---|---|---|
| `POST /conversations/{id}/messages` | 20 requests per minute per user (sliding window) | `429 RATE_LIMITED`, `Retry-After: <seconds>` |
| Upload endpoints | 5 MB per file, 20 000 rows | `413 FILE_TOO_LARGE` / `400 IMPORT_FILE_INVALID` |
| Assistant question | 1 000 characters | `400 VALIDATION_FAILED` |

The limiter is in memory per instance (one instance in the current deployment).

## Idempotency

| Operation | Idempotent? | Notes |
|---|---|---|
| `GET`, `PUT`, `DELETE` | yes | Repeating a `DELETE` returns `404` after the first success |
| `PATCH /transactions/{id}` | yes | Setting the same category twice has the same effect |
| `POST /imports` | **effectively yes** | Each row has a fingerprint (user, date, amount, currency, normalised description, occurrence number). Re-uploading a file stores nothing new and reports the rows as `duplicates`; two identical purchases inside one file are kept |
| `POST /conversations/{id}/messages` | no | Retrying stores the question again and calls the model again. Retry only on `503`/network errors |
| `POST` create endpoints | no | Duplicates are rejected with `409 CONFLICT` where a natural key exists (category name, budget per category and currency, mapping name, e-mail) |

## Timeouts and retries

The LLM call has a 20 s timeout and **one** automatic retry on timeouts, `429` and `5xx` from the provider; then the
API answers `503 AI_UNAVAILABLE`. Imports never fail because of the AI: transactions are stored as uncategorised and
re-categorised by a background job every 30 minutes.
