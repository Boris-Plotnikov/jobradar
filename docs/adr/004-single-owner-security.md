# ADR-004: One owner, session login, public showcase

- **Status:** Accepted
- **Date:** 2026-11-18

## Decision

- Spring Security with one in-memory user from environment variables (BCrypt hash at startup).
- JSON login endpoint, session cookie `JOBRADAR_SESSION` (httpOnly, SameSite=Lax), session id changed after login.
- CSRF protection with a cookie token (`XSRF-TOKEN` → header `X-XSRF-TOKEN`) — needed because the session is a cookie.
- `/api/public/**` is open and returns only non-personal fields; everything else under `/api` needs login (401 JSON, no redirect).
- Login rate limit: 10 failed attempts per IP in 15 minutes.

## Consequences

- No user table needed. More users would mean a `users` table like in FinTrack.
