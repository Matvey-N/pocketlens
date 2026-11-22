# PocketLens

**Web application for automated personal expense tracking with an AI assistant** — bachelor diploma project.

PocketLens imports CSV statements from any bank, categorises transactions automatically (your rules first, AI for
unknown merchants, "needs review" when unsure), warns before a budget is exceeded, detects subscriptions and answers
questions such as *"How much did I spend on eating out last month?"*. The AI only chooses which database query to
run — every figure is computed by SQL and the answer links to the exact transactions behind it.

[![CI/CD](https://github.com/Matvey-N/pocketlens/actions/workflows/ci.yml/badge.svg)](https://github.com/Matvey-N/pocketlens/actions/workflows/ci.yml)

| | |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Spring Framework 7), Spring Security 7 (JWT), Spring Data JPA / Hibernate, PostgreSQL 16, Flyway, Redis cache, Actuator + Prometheus |
| Frontend | React 19, TypeScript, Vite, Material UI, Redux Toolkit + RTK Query, React Router, React Hook Form + Zod, Recharts |
| AI | Anthropic Claude (Messages API, tool use) behind an `LlmClient` interface; optional |
| Quality | JUnit 5, Mockito, Testcontainers, JaCoCo (≥ 80 % gate), Vitest, Testing Library, MSW, Playwright, ESLint, Prettier, Redocly |
| Delivery | Docker, Docker Compose, GitHub Actions → GitHub Container Registry, Trivy scan, deploy hook |

## Quick start

```bash
cp .env.example .env            # set DB_PASSWORD and JWT_SECRET (≥ 32 chars); AI_API_KEY is optional
docker compose up --build -d
```

* App: http://localhost:8081 — register, then import [`docs/examples/sample-generic.csv`](docs/examples/sample-generic.csv)
* API reference (Swagger UI): http://localhost:8081/swagger-ui.html
* Monitoring (optional): `docker compose --profile monitoring up -d` → Grafana http://localhost:3000

Development without Docker:

```bash
# backend (needs PostgreSQL on localhost:5432, db/user/password "pocketlens")
cd backend && mvn spring-boot:run            # http://localhost:8080
# frontend
cd frontend && npm ci && npm run dev         # http://localhost:5173 (proxies /api to :8080)
```

Tests: `cd backend && mvn verify` (unit + integration tests with Testcontainers, needs Docker; coverage report in
`backend/target/site/jacoco/index.html`) · `cd frontend && npm test && npm run build && npm run e2e`.

## Repository structure

```
backend/      Spring Boot REST API (modules: auth, user, importing, categorization, transaction, category,
              budget, dashboard, subscription, assistant, admin, ai, common) + Flyway migrations + tests
frontend/     React SPA (features/, shared/, app/, layout/) + unit tests + Playwright e2e + nginx image
docs/         reference/ (OpenAPI), guides/, concepts/, architecture/, frontend/, examples/
infra/        Prometheus and Grafana configuration
.github/      CI/CD pipeline
```

## Where each diploma criterion is implemented

### Back-end

| Requirement | Where |
|---|---|
| Modern framework, DB, ORM | Spring Boot 4.1 · PostgreSQL · JPA/Hibernate entities in `*/domain`, repositories in `*/persistence`, Flyway `backend/src/main/resources/db/migration` |
| Layered architecture, modularity | Every module has `api` → `service` → `domain`/`persistence` packages — see [architecture](docs/architecture/overview.md) |
| SOLID | Extension points `StatementFormat` (new bank = one class) and `AssistantTool`; `LlmClient` interface (DIP); single-purpose `CategoryUsageCleaner` components |
| API description | [`docs/reference/openapi.yaml`](docs/reference/openapi.yaml), served at `/swagger-ui.html`; `OpenApiContractIT` checks it against the code |
| Global error handling, HTTP status codes | `common/error/GlobalExceptionHandler`, `ProblemDetailsSecurityHandler`, `ErrorCode` (RFC 9457) |
| Structured logging | JSON (ECS) logs, `common/web/CorrelationIdFilter` (correlation id in MDC and `X-Correlation-Id`), business events (`statement_imported`, `llm_call`, …) |
| Unit tests ≥ 70 % | 136 unit tests; JaCoCo gate **80 %** on the service layer (`mvn verify` fails below); current ≈ 98 % (see CI summary) |
| Integration tests | `backend/src/test/java/com/pocketlens/it` — Testcontainers PostgreSQL: auth flow, data isolation, full journey, assistant, contract |
| Security | BCrypt(12), standard JWT (Nimbus), refresh-token rotation, Bean Validation on every input, user-scoped queries, no secrets in code (`.env.example`), PII sanitiser before AI calls |
| Efficient queries | Aggregations in SQL (`TransactionAggregates`), entity graphs against N+1, batch inserts, indexes per access path (`V1__schema.sql`) |
| Caching | Redis dashboard cache (`CacheConfig`, `DashboardService`) |
| Monitoring | Actuator health/probes, `/actuator/prometheus`, Prometheus + Grafana in `docker-compose.yml` |
| CI/CD, quality gates | `.github/workflows/ci.yml`: tests + coverage gate, images to GHCR, Trivy scan, deploy only after all checks pass |
| Production deployment | Docker images built by CI; see [Deployment](#deployment) |

### API documentation

| Requirement | Where |
|---|---|
| Industry-standard spec, validated, in VCS | [`docs/reference/openapi.yaml`](docs/reference/openapi.yaml) (OpenAPI 3.0.3), Redocly lint in CI ([`docs/redocly.yaml`](docs/redocly.yaml)) |
| Examples, status codes, errors, data models | Every operation in the spec: request/response examples, all status codes with problem examples, schemas |
| Getting started + integration tutorial | [`docs/guides/getting-started.md`](docs/guides/getting-started.md), [`docs/guides/tutorial-import-and-ask.md`](docs/guides/tutorial-import-and-ask.md), runnable [`docs/examples/walkthrough.sh`](docs/examples/walkthrough.sh) |
| Architecture overview | [`docs/architecture/overview.md`](docs/architecture/overview.md): context, components, sequence diagrams, data model |
| Published format | Swagger UI at `/swagger-ui.html`; Redoc HTML built in CI (artifact `api-reference`); Markdown docs in [`docs/`](docs/README.md) |
| Strategy, conventions, versioning, gaps | [`docs/README.md`](docs/README.md), [`docs/concepts/versioning.md`](docs/concepts/versioning.md), [`CHANGELOG.md`](CHANGELOG.md) |
| Advanced topics | [Authentication](docs/concepts/authentication.md), [error taxonomy](docs/concepts/errors.md), [pagination](docs/concepts/pagination-and-filtering.md), [rate limits & idempotency](docs/concepts/rate-limits-and-idempotency.md) |

### Front-end

| Requirement | Where |
|---|---|
| SPA, modern framework, bundler | React 19 + TypeScript + Vite (`frontend/`) |
| Architecture diagram, user flows, stack justification | [`docs/frontend/architecture.md`](docs/frontend/architecture.md) |
| Components / pages / services | `src/shared/ui`, `src/features/*/components`, `src/features/*/*Page.tsx`, `src/shared/api` + `src/features/*/*Api.ts` |
| Routing (lazy) and guards | `src/app/router.tsx`, `src/features/auth/RequireAuth.tsx` |
| Global + local state | Redux Toolkit slices (`auth`, `transactionFilters`, `ui`, `notifications`), RTK Query cache, memoised selectors |
| Forms with validation | React Hook Form + Zod: login, registration, budgets, category correction, column mapping, categories |
| Tables, filtering, search, sorting | `TransactionsPage` (`TransactionTable`, `TransactionFilters`) |
| Global error handling | `shared/api/httpClient.ts` (401 refresh, normalisation), `app/errorMiddleware.ts` (toasts), `shared/ui/ErrorBoundary.tsx` |
| Loading states | Skeletons for every async view (`shared/ui/LoadingSkeleton.tsx`), typing indicator in the assistant |
| Responsive, accessible, dark mode | Drawer/card layouts from 360 px, ARIA labels, keyboard dropzone, light/dark theme |
| Charts / dashboard | Recharts donut and bar charts (`features/dashboard/components`) |
| Unit tests (≥ 3) + e2e (1–2) | 16 Vitest tests in `src/**/*.test.ts(x)`; 3 Playwright scenarios × desktop/mobile in `frontend/e2e` |
| Lint and formatting | ESLint (typescript-eslint, react-hooks) + Prettier, enforced in CI |

## Deployment

The application is two containers (backend, frontend/nginx) plus PostgreSQL and Redis. CI publishes
`ghcr.io/matvey-n/pocketlens-backend` and `ghcr.io/matvey-n/pocketlens-frontend` on every push to `main`. On a server
with Docker:

```bash
cp .env.example .env    # production values: strong secrets, COOKIE_SECURE=true
BACKEND_IMAGE=ghcr.io/matvey-n/pocketlens-backend:latest \
FRONTEND_IMAGE=ghcr.io/matvey-n/pocketlens-frontend:latest docker compose up -d --no-build
```

Put HTTPS in front (e.g. Caddy or the hosting platform's TLS). Configure the `DEPLOY_HOOK_URL` repository secret to let
the pipeline trigger the deployment automatically after all checks pass.

## AI assistance disclosure

Parts of the code and documentation were drafted with the help of an AI assistant (Claude) and reviewed, tested and
adapted by the author. All design decisions are documented in `docs/`.
