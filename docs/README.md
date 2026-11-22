# PocketLens documentation

| Folder | Content |
|---|---|
| [`reference/`](reference/openapi.yaml) | **OpenAPI 3.0 contract** — single source of truth for the API (served by Swagger UI at `/swagger-ui.html`, rendered with Redoc in CI) |
| [`guides/`](guides/) | [Getting started](guides/getting-started.md) · [Tutorial: import a statement and ask the assistant](guides/tutorial-import-and-ask.md) |
| [`concepts/`](concepts/) | [Authentication](concepts/authentication.md) · [Errors](concepts/errors.md) · [Pagination, filtering, sorting](concepts/pagination-and-filtering.md) · [Rate limits and idempotency](concepts/rate-limits-and-idempotency.md) · [Versioning](concepts/versioning.md) |
| [`architecture/`](architecture/overview.md) | System context, components, request flows, data model, decisions |
| [`frontend/`](frontend/architecture.md) | SPA architecture, state management, user flows, tech stack justification |
| [`examples/`](examples/) | Sample CSV statements and a runnable `curl` walkthrough |
| [`../CHANGELOG.md`](../CHANGELOG.md) | Release notes |

## Documentation strategy

* **Design-first, one source of truth.** The API is described in `reference/openapi.yaml` (OpenAPI 3.0.3, YAML,
  version-controlled). The backend serves this exact file in Swagger UI, so the documentation that developers read
  is the one that is tested.
* **Validated in CI.** Every push runs `redocly lint` (recommended ruleset plus stricter rules: operationId, summary,
  4xx responses, tag descriptions, license, no unused components) and builds a static Redoc page.
* **Contract test against the code.** `OpenApiContractIT` compares the operations implemented by the controllers
  (springdoc runtime model) with the documented ones; the build fails if an endpoint is undocumented or a documented
  endpoint does not exist.
* **Examples are realistic** — taken from integration-test scenarios (same merchants, amounts and error bodies) and
  cover typical and edge cases (missing columns, invalid rows, rate limit, AI unavailable).
* **Naming conventions.** Paths are plural nouns in kebab-case (`/import-profiles`), JSON properties in camelCase,
  enum values in UPPER_SNAKE_CASE, error codes stable UPPER_SNAKE_CASE, operationIds in lowerCamelCase verbs
  (`listTransactions`, `updateTransactionCategory`).
* **Versioning.** URI major version (`/api/v1`), semantic version in `info.version`, changes recorded in
  `CHANGELOG.md` (see [versioning](concepts/versioning.md)).
* **Formatting.** Markdown with one H1 per page, tables for reference data, Mermaid for diagrams (rendered by GitHub).

### Known gaps and limitations

* The tutorial uses `curl`; there is no generated SDK.
* Response schemas of the data export are described loosely (`additionalProperties`) because the export is meant for
  humans and other tools, not for programmatic round-trips.
* No hosted sandbox: a local stack is started with Docker Compose in one command (see Getting started).
* Error examples show English messages only; the `code` field is the stable contract, messages may change.
