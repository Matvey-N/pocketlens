# Versioning

* **Major version in the URI:** `/api/v1`. A breaking change (removing/renaming a field or endpoint, changing a type
  or the meaning of a value) creates `/api/v2`; v1 stays available for at least 6 months after v2 is released and
  responses of deprecated endpoints carry a `Deprecation` header.
* **Non-breaking changes** (new endpoints, new optional fields, new enum values in responses, new error codes) are
  added to v1. Clients must ignore unknown fields and handle unknown enum values gracefully.
* **Contract version:** semantic version in `info.version` of `openapi.yaml` (currently 1.0.0) —
  MAJOR = URI version, MINOR = additions, PATCH = documentation fixes.
* **Release notes:** every change is recorded in [`CHANGELOG.md`](../../CHANGELOG.md) (Keep a Changelog format).
* **Database:** schema changes are versioned Flyway migrations (`V1__…sql`), applied automatically on start-up.
