# Errors

Every error is returned as **RFC 9457 problem details** (`Content-Type: application/problem+json`) with a stable
machine-readable `code`:

```json
{
  "type": "https://pocketlens.app/problems/validation-failed",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "code": "VALIDATION_FAILED",
  "errors": [{ "field": "password", "message": "size must be between 10 and 72" }],
  "correlationId": "3e4f5a6b-7c8d-4e9f-a0b1-c2d3e4f5a6b7",
  "timestamp": "2026-11-22T10:15:30Z"
}
```

* Switch on `code`, not on `detail` (texts may change).
* `correlationId` is also returned in the `X-Correlation-Id` header and appears in every server log line of the
  request — quote it when reporting a problem. Clients may send their own `X-Correlation-Id` (8–64 chars `[A-Za-z0-9-]`).
* Stack traces and internal messages are never returned.

## Error taxonomy

| HTTP | Code | Meaning | Recovery |
|---|---|---|---|
| 400 | `VALIDATION_FAILED` | Field rules violated; see `errors[]` | Fix the fields |
| 400 | `MALFORMED_REQUEST` | Body is not JSON, wrong parameter type, missing part | Fix the request |
| 400 | `IMPORT_FILE_INVALID` | Empty, not CSV, unreadable, no data rows, > 20 000 rows | Export the statement again as CSV / split it |
| 401 | `UNAUTHORIZED` | Missing/expired/invalid access token | `POST /auth/refresh`, then retry |
| 401 | `INVALID_CREDENTIALS` | Wrong e-mail or password (login, account deletion) | Ask the user again |
| 401 | `INVALID_REFRESH_TOKEN` | Session expired or refresh token re-used | Log in again |
| 403 | `FORBIDDEN` | Role missing (admin endpoints) | — |
| 404 | `NOT_FOUND` | Resource does not exist **or belongs to another user**; unknown path | Check the id |
| 405 | `METHOD_NOT_ALLOWED` | HTTP method not supported on this path | Use the documented method |
| 409 | `CONFLICT` | Duplicate e-mail, category name, budget, mapping name | Choose another value |
| 413 | `FILE_TOO_LARGE` | Upload > 5 MB | Split the file |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Wrong `Content-Type` | Use JSON / multipart |
| 422 | `IMPORT_COLUMNS_MISSING` | File does not match the format; `missingColumns`, `foundColumns` | Choose another format or save a mapping |
| 422 | `IMPORT_ROWS_INVALID` | Invalid values; `rowErrors[]` (row number, message, max 20). Nothing imported | Fix the rows or the date/decimal format of the mapping |
| 422 | `PROMPT_REJECTED` | Question matched the prompt-injection filter | Rephrase |
| 429 | `RATE_LIMITED` | > 20 assistant questions per minute; `Retry-After` header and `retryAfterSeconds` | Wait and retry |
| 500 | `INTERNAL_ERROR` | Unexpected error (logged with the correlation id) | Retry later, report the id |
| 503 | `AI_UNAVAILABLE` | LLM provider not configured, timed out or failed after one retry | Retry later; data is kept |

All operations document their possible errors in the OpenAPI contract.
