# Pagination, filtering and sorting

Collections that can grow large (`GET /transactions`) are paginated with **page/size**:

| Parameter | Default | Limits |
|---|---|---|
| `page` | `0` | zero-based |
| `size` | `20` | 1–100 |
| `sort` | `date,desc` | `date`, `amount`, `merchant` × `asc`/`desc` (whitelist; ties are broken by import time) |

Response envelope:

```json
{ "items": [ … ], "page": 0, "size": 20, "totalItems": 143, "totalPages": 8 }
```

**Why offset pagination?** Lists are per user (thousands, not millions of rows) and users jump to pages and sort by
different columns; offset paging supports this with indexed queries (`(user_id, booking_date)`). Keyset pagination
would be preferred for infinite feeds over very large tables.

## Filters (combined with AND)

`from`, `to` (ISO dates, inclusive) · `categoryId` · `uncategorized=true` · `needsReview=true` ·
`q` (case-insensitive text in merchant or description, max 100 chars) · `currency` (ISO code).

```http
GET /api/v1/transactions?from=2026-11-01&to=2026-11-30&q=lidl&sort=amount,asc&page=0&size=50
```

Small, bounded collections (categories, rules, budgets, conversations, the last 20 imports) are returned as plain arrays.
