# Frontend brief (specification for the frontend developer)

Written by Boris Plotnikov as the task for the external frontend developer. The backend API is documented in Swagger (`/swagger-ui.html`).

## Stack and rules

- React 18 + TypeScript, Vite, Tailwind CSS, Recharts. No UI component library.
- `HashRouter` and relative URLs (`base: "./"`): the app must work under any path and subdomain.
- Build output goes to `backend/src/main/resources/static` — one JAR serves API and UI.
- Colours only through CSS variables in `index.css` (brand, accent, ink, muted, surface) — the brand palette will change.
- Every POST/PUT/PATCH/DELETE sends the CSRF token from the `XSRF-TOKEN` cookie in `X-XSRF-TOKEN`.
- Errors from the API come as `{"error": "..."}` and must be shown to the user as they are.
- Mobile first: everything usable at 375 px width.

## Screens

| Screen | Content | API |
|---|---|---|
| Login | Username, password, link to the showcase | `POST /api/auth/login`, `GET /api/auth/me` |
| Feed | Cards with score badge (AI / keyword), filters: search, country, work mode, direction, source, period, min score, sort; "Run pipeline now" with progress | `GET /api/vacancies`, `GET /api/vacancies/filters`, `POST /api/pipeline/run`, `GET /api/pipeline/runs` |
| Vacancy | Big score, why (strengths, gaps, tip), description, link to original with source name, status select, hide, score again, cover letter panel | `GET /api/vacancies/{id}`, `POST …/rescore`, `POST …/hide`, letters API |
| Cover letter | EN/DE switch, generate, version select, edit, save as new version, copy | `GET/POST /api/vacancies/{id}/letters`, `POST …/letters/generate` |
| Applications | 5 columns, drag and drop (select on mobile), notes, follow-up warning | `GET/POST/PATCH/DELETE /api/applications` |
| Market insights | KPI tiles, skills bar chart (in CV vs missing), countries, pies, weekly trend, salaries, skill gaps | `GET /api/insights?days=` |
| Stats | Funnel tiles, AI spending, weekly bars, pipeline runs with log | `GET /api/stats` |
| Settings | CV, directions, countries, keywords, stop words, digest; re-check button; letter templates | `GET/PUT /api/settings`, `POST /api/pipeline/refilter`, `GET/PUT /api/templates` |
| Showcase (public) | Hero with counters, "How it works" in 5 steps, best matches, insights | `GET /api/public/*` |

## Acceptance

- `npm run build` passes `tsc` without errors.
- No personal data on the showcase.
- All texts in English, short and plain.
