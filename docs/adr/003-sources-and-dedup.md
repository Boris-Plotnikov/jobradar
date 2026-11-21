# ADR-003: Official job APIs and two-step duplicate detection

- **Status:** Accepted
- **Date:** 2026-11-18

## Context

Big job boards forbid scraping in their terms. The same job appears on several boards with small differences in the title.

## Decision

- Only official public APIs without keys: Arbeitnow, Remotive, Jobicy, Himalayas. Himalayas requires a visible credit and its own application link; both are used. Each source is one class behind `JobSource`; a new source is one new class, Spring injects all of them as `List<JobSource>`.
- Duplicates: (1) SHA-256 fingerprint of normalized company + title + country — removes "(m/w/d)", "GmbH", punctuation; (2) same company in the last 30 days with title Jaccard similarity ≥ 0.8.

## Consequences

- Legal and stable data; every job links to the original ad with the source name.
- Some duplicates with a very different title remain — acceptable, the user can hide them.
