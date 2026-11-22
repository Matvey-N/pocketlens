# Getting started

This guide takes you from zero to your first authenticated API call in about five minutes.

## 1. Start PocketLens

Requirements: Docker with Compose.

```bash
git clone https://github.com/Matvey-N/pocketlens.git && cd pocketlens
cp .env.example .env          # set DB_PASSWORD and JWT_SECRET (≥ 32 characters)
docker compose up --build -d
```

| URL | What |
|---|---|
| http://localhost:8081 | Web application |
| http://localhost:8081/swagger-ui.html | Interactive API reference (Swagger UI) |
| http://localhost:8081/api/v1 | API base URL |

> AI features need `AI_API_KEY` in `.env`. Without a key everything works except AI categorisation and the
> assistant, which answer `503 AI_UNAVAILABLE`.

Without Docker: start PostgreSQL, then `cd backend && mvn spring-boot:run` (API on port 8080) and
`cd frontend && npm ci && npm run dev` (UI on http://localhost:5173, proxies `/api` to 8080).

## 2. Register

```bash
BASE=http://localhost:8081/api/v1
VERSION=$(curl -s $BASE/privacy-notice | jq -r .version)

curl -s -c cookies.txt -X POST $BASE/auth/register \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"anna@example.com\",\"password\":\"Spring2026secure\",\"displayName\":\"Anna\",
       \"acceptPrivacyNotice\":true,\"privacyNoticeVersion\":\"$VERSION\"}" | tee auth.json
```

The response contains `accessToken` (valid 15 minutes); the refresh token is stored in `cookies.txt` as the
`pl_refresh` cookie.

## 3. Call the API

```bash
TOKEN=$(jq -r .accessToken auth.json)
curl -s $BASE/users/me -H "Authorization: Bearer $TOKEN"
```

```json
{ "id": "b3f4e5d6-…", "email": "anna@example.com", "displayName": "Anna", "role": "USER", … }
```

## 4. Keep the session alive

When a call returns `401 UNAUTHORIZED`, exchange the cookie for a new token (the cookie is rotated):

```bash
curl -s -b cookies.txt -c cookies.txt -X POST $BASE/auth/refresh | jq -r .accessToken
```

## Next steps

* [Tutorial: import a statement and ask the assistant](tutorial-import-and-ask.md)
* [Authentication in depth](../concepts/authentication.md) · [Error handling](../concepts/errors.md)
