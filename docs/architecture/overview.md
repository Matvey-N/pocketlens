# Architecture overview

## System context

```mermaid
flowchart LR
    U([User<br/>browser / phone]) -->|HTTPS| N[nginx<br/>SPA + reverse proxy]
    A([Administrator]) -->|HTTPS| N
    N -->|/api| B[PocketLens API<br/>Spring Boot]
    B -->|JPA / Flyway| P[(PostgreSQL)]
    B -->|cache| R[(Redis)]
    B -->|HTTPS, tool use<br/>no names / IBANs| L[[LLM provider<br/>Anthropic Claude]]
    PR[Prometheus] -->|/actuator/prometheus| B
    G[Grafana] --> PR
    BANK([Bank]) -. CSV export .-> U
```

## Backend: modular monolith with layered modules

Each business module is a package with the same four layers; dependencies point downwards only.

```mermaid
flowchart TB
    subgraph module["Every module (auth, importing, transaction, categorization, budget, dashboard, subscription, assistant, admin, user, category)"]
      API["api — REST controllers + DTOs (validation, HTTP status)"] --> SVC["service — business logic (transactions, rules)"]
      SVC --> DOM["domain — JPA entities with behaviour"]
      SVC --> PER["persistence — Spring Data repositories, criteria queries"]
    end
    COMMON["common — security, global error handling, correlation-id logging, configuration"]
    AI["ai.llm — LlmClient port + Anthropic adapter"]
    SVC --> AI
```

| Module | Responsibility | Key classes |
|---|---|---|
| auth / user | Registration with consent, login, JWT + rotating refresh tokens, export, deletion | `AuthService`, `TokenService`, `UserService` |
| importing | CSV decoding, bank formats, column mappings, de-duplication | `ImportService`, `StatementFormat` (+ Revolut, Wise, EU, generic, mapped) |
| categorization | Merchant normalisation, rules, AI classifier, PII removal, retry job | `CategorizationService`, `RuleEngine`, `AiCategorizer`, `PiiSanitizer` |
| transaction | Search/filter/sort, corrections with rules, DB aggregations | `TransactionService`, `TransactionAggregates`, `TransactionSpecifications` |
| budget | Budgets, linear projection, warnings | `BudgetService`, `BudgetProjection` |
| dashboard | Cached read model per user and month | `DashboardService` |
| subscription | Recurring-payment detection | `SubscriptionDetector` |
| assistant | Tool-calling loop, tools, prompt guard, rate limit, conversations | `AssistantService`, `AssistantTool` implementations |
| admin | Default categories, global rules, usage statistics | `AdminController`, `AdminStatsService` |

**SOLID in practice:** one responsibility per service (e.g. `CategoryUsageCleaner` implementations per module);
open/closed extension points (`StatementFormat`, `AssistantTool` — a new bank or assistant capability is one new
class); business logic depends on the `LlmClient` interface, not on the provider (dependency inversion, mocked in
tests); small interfaces (`CategoryUsageCleaner` has one method).

## Request flow: asking the assistant (UC-2)

```mermaid
sequenceDiagram
    actor U as User
    participant C as ConversationController
    participant S as AssistantService
    participant G as PromptGuard + RateLimiter
    participant L as LLM (Claude)
    participant T as Tools (SQL, user-scoped)
    participant D as PostgreSQL
    U->>C: POST /conversations/{id}/messages
    C->>S: ask(userId, id, question)
    S->>G: length, injection patterns, 20/min
    S->>D: store question
    S->>L: question + history + tool definitions
    L-->>S: tool_use get_spending_total(from, to, category)
    S->>T: validate arguments, execute
    T->>D: SUM(...) WHERE user_id = :me ...
    D-->>T: totals + transaction ids
    T-->>S: result (no names / IBANs)
    S->>L: tool_result
    L-->>S: final text
    S->>D: store answer + interpretation + source ids + token usage
    C-->>U: 201 {question, answer}
```

## Request flow: importing a statement (UC-1)

```mermaid
sequenceDiagram
    actor U as User
    participant I as ImportService
    participant F as StatementFormat
    participant D as PostgreSQL
    participant K as CategorizationService
    participant L as LLM
    U->>I: POST /imports (file, format)
    I->>I: validate size/type, decode charset
    I->>F: check required columns, parse rows
    alt columns missing / invalid rows
        I-->>U: 422 (nothing imported)
    end
    I->>D: existing fingerprints?
    I->>D: save new transactions
    I->>K: categorise
    K->>K: rules (user first, longest pattern)
    K->>L: unknown merchants (sanitised, batches of 40)
    alt AI unavailable
        K->>D: mark uncategorised (retry job every 30 min)
    end
    I-->>U: 201 summary (read / new / duplicates / review)
```

## Data model

```mermaid
erDiagram
    USERS ||--o{ TRANSACTIONS : owns
    USERS ||--o{ CATEGORIES : "custom"
    USERS ||--o{ MERCHANT_RULES : "learned"
    USERS ||--o{ BUDGETS : sets
    USERS ||--o{ IMPORT_BATCHES : uploads
    USERS ||--o{ IMPORT_PROFILES : maps
    USERS ||--o{ CONVERSATIONS : has
    USERS ||--o{ REFRESH_TOKENS : sessions
    CONVERSATIONS ||--o{ MESSAGES : contains
    CATEGORIES ||--o{ TRANSACTIONS : classifies
    CATEGORIES ||--o{ MERCHANT_RULES : target
    CATEGORIES ||--o{ BUDGETS : limits
    BUDGETS ||--o{ BUDGET_WARNINGS : issued
    IMPORT_BATCHES ||--o{ TRANSACTIONS : created
```

All user-owned tables cascade on user deletion (GDPR Art. 17). Indexes follow the access paths:
`(user_id, booking_date)`, `(user_id, category_id, booking_date)`, `(user_id, merchant)`, partial indexes for the
review queue and waiting transactions, unique `(user_id, fingerprint)` for de-duplication.

## Cross-cutting concerns

| Concern | Implementation |
|---|---|
| Error handling | `GlobalExceptionHandler` + security entry point → RFC 9457 problem details with stable codes |
| Logging | Structured JSON (ECS) to stdout; correlation id per request in MDC and `X-Correlation-Id`; no transaction texts or chat content in logs |
| Caching | Dashboard read model in Redis (10 min TTL, evicted on every data change) |
| Monitoring | Actuator health/liveness/readiness, Micrometer → Prometheus → Grafana |
| Configuration | Environment variables only (`.env.example`); secrets never in the repository |
| Security | BCrypt(12), JWT HS256 (Nimbus), refresh-token rotation, input validation, user-scoped queries, PII sanitiser before LLM calls, prompt-injection filter, rate limit |

## Deployment and CI/CD

```mermaid
flowchart LR
    DEV[git push main] --> CI{GitHub Actions}
    CI --> BE[Backend: unit + Testcontainers ITs<br/>JaCoCo ≥ 80% gate]
    CI --> FE[Frontend: lint, types, Vitest, build, Playwright]
    CI --> DOC[OpenAPI lint + Redoc]
    BE & FE & DOC --> IMG[Docker images → GHCR<br/>Trivy scan]
    IMG --> DEP[Deploy hook → production<br/>secrets from environment]
```

## Key decisions

| Decision | Alternatives | Reason |
|---|---|---|
| Modular monolith | Microservices | One developer, one deployable; modules keep boundaries so services can be extracted later |
| LLM chooses tools, SQL computes | LLM reads raw transactions | Correct, verifiable numbers; less personal data sent; cost independent of data volume |
| Own Anthropic client (Spring `RestClient`) behind `LlmClient` | Spring AI | Explicit tool loop, retries and PII control; no extra framework; provider replaceable |
| Hand-written OpenAPI as contract + contract test | Code-generated docs only | Documentation written for people, still verified against code |
| JWT in memory + HttpOnly refresh cookie | Token in localStorage | Not readable by injected scripts |
