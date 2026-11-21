# Requirements

## Business goal

Find suitable junior / working student developer jobs in Europe with less than 15 minutes of manual work per day, and learn which skills the market wants.

## User

One owner (job seeker). Visitors (employers) see a public showcase without personal data.

## User stories

1. **Daily collection.** As the owner I want new vacancies collected every morning, so I do not visit job boards myself.
   - Given the schedule 06:00 Europe/Vienna, when the pipeline runs, then vacancies from all enabled sources are stored and a run record shows fetched / new / duplicates / relevant / scored.
2. **No duplicates.** As the owner I want each job only once.
   - Given "Java Developer (m/w/d)" at "Acme GmbH" and "Java Developer m/f/d" at "ACME" in the same country, then only the first is stored.
3. **Irrelevant jobs filtered for free.** As the owner I want senior and foreign-country jobs dropped without AI cost.
   - Given the stop word "senior", when a title contains "Senior", then the vacancy is stored as rejected with the reason "Title contains stop word 'senior'" and is not sent to AI.
4. **Explained score.** As the owner I want a score with strengths and gaps, so I can decide in seconds.
   - Given an API key and budget, then the score comes from AI; without them from keywords — and the UI shows which.
5. **Budget control.** As the owner I want AI spending capped per month.
   - Given a monthly budget of 5 USD, when the sum of logged costs reaches it, then new scoring uses keywords until next month.
6. **Applications board.** As the owner I want to track applications and get a reminder after 10 days without answer.
7. **Cover letters.** As the owner I want a draft letter in English or German, edit it and keep versions.
8. **Market insights.** As the owner I want to see the most requested skills and which of them are missing in my CV.
9. **Showcase.** As an employer I want to see that the system works, without logging in.
   - Given the public page, then no application statuses, notes, letters or CV text are returned by `/api/public/*`.

## Non-functional

- One small server (JVM `-Xmx256m`), one pipeline run at a time.
- All secrets in environment variables.
- Every external call has timeouts; one failing source does not stop the others.
