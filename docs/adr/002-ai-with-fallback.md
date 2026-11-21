# ADR-002: AI scoring with structured output, budget and fallback

- **Status:** Accepted
- **Date:** 2026-11-18

## Context

Reading a vacancy and comparing it with a CV is a language task — rules alone give poor results. But AI costs money, can fail and can return unexpected text.

## Decision

- Cheap rules first (prefilter): about 90% of vacancies never reach the AI.
- `claude-haiku-5-5` for scoring, `claude-sonnet-5-5` for letters, via the official Anthropic Java SDK.
- **Structured output:** the answer must match the `MatchResult` record (score, summary, strengths, gaps, advice). The SDK builds the JSON schema from the record; `validated()` checks the range before saving.
- **Prompt caching:** instructions + CV are the system prompt and marked for caching — the same for every vacancy.
- **Budget:** every call is stored in `ai_usage` with tokens and cost; when the monthly sum reaches `AI_MONTHLY_BUDGET_USD`, scoring switches to `KeywordMatchScorer`. Same switch without an API key or on any AI error.

## Consequences

- The service always works, with or without AI; the UI shows how each vacancy was scored.
- Cost at ~50 relevant vacancies per day: a few cents per day.
- Next step at higher volume: Message Batches API (50% cheaper, results within 24 h) for the nightly run.
