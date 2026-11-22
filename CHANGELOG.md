# Changelog

All notable changes of the PocketLens API and application. Format: [Keep a Changelog](https://keepachangelog.com),
versions follow [Semantic Versioning](https://semver.org) (see `docs/concepts/versioning.md`).

## [1.0.0] — 2026-11-22

### Added
- Authentication: registration with privacy-notice consent, login, JWT access tokens, rotating HttpOnly refresh
  tokens with reuse detection, logout.
- CSV import with built-in formats (generic, Revolut, Wise, EU debit/credit), saved column mappings, file preview,
  de-duplication and import history.
- Categorisation: merchant normalisation, global and learned user rules, AI classification with confidence threshold
  0.8 and "needs review", PII removal before AI calls, background retry when AI is unavailable.
- Transactions: filtering, text search, sorting, pagination, category correction with "always use for this merchant".
- Custom categories; monthly budgets with linear projection and early warnings.
- Dashboard per currency with category breakdown, six-month trend and budgets (Redis cache).
- Subscription detection with price-increase flag.
- AI assistant with tool calling, interpretation, source transactions, conversation history, prompt guard and rate limit.
- Data export and account deletion; administration of default categories, global rules and usage statistics.
- OpenAPI 3 contract, guides, concept pages, architecture documentation, contract test.
- CI/CD: tests with coverage gate, OpenAPI lint, Docker images to GHCR, image scan, deployment hook.
