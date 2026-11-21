INSERT INTO settings (id, resume_text, directions, countries, keywords, stop_words, min_score, digest_hour, remote_only, updated_at)
VALUES (1,
'Boris Plotnikov - Junior Java Developer / Working Student (Werkstudent).
Vienna, Austria. Available up to 20 hours per week, remote or hybrid.

Education: BSc Computer Science, University of Vienna (since 2024). Master of Economics. Bachelor of Law, Bachelor of Economics.

Programming: Java (Spring Boot, JPA, REST APIs, JUnit), C++ (STL, CMake, GoogleTest), SQL (PostgreSQL), Git, Docker basics, GitHub Actions.
Portfolio: JobRadar (Java/Spring Boot, Claude API), FinTrack (accounting, Java/Spring), ProcureDesk (purchase approvals, Java/Spring), StockKeeper (C++ warehouse CLI), ViennaRoute (C++ Dijkstra route planner).

Experience: 2019-2025 Head of Procurement and IT Manager at a gas transportation company: procurement, contracts, process control, team of 3. 2017-2018 tax authority, supply and IT departments.

Languages: Russian (native), English (C2), German (C1).',
'Junior Java Developer
Junior Backend Developer
Working Student Software Development
Junior C++ Developer
Junior Software Engineer',
'AT
DE
CH
NL
EU',
'java
spring
backend
c++
werkstudent
working student
junior
software engineer
software developer
entwickler',
'senior
lead
principal
staff engineer
head of
5+ years
7+ years
10+ years
architect',
60, 8, FALSE, CURRENT_TIMESTAMP);

INSERT INTO prompt_templates (code, title, body, updated_at) VALUES
('COVER_LETTER_EN', 'Cover letter (English)',
'Write a cover letter in English for the vacancy below, using the candidate CV.
Rules:
- 180 to 250 words, three short paragraphs, no headings.
- Start with why this company and role, not with "I am writing to apply".
- Mention 2 or 3 concrete facts from the CV that match the vacancy (projects, technologies, experience).
- Be honest: do not invent experience the CV does not contain.
- Mention availability: up to 20 hours per week as a working student.
- Plain, confident business English. No clichés like "passionate", "dynamic", "team player".
- End with a simple sentence offering a call.',
CURRENT_TIMESTAMP),
('COVER_LETTER_DE', 'Anschreiben (Deutsch)',
'Schreibe ein Anschreiben auf Deutsch für die unten stehende Stelle, basierend auf dem Lebenslauf.
Regeln:
- 180 bis 250 Wörter, drei kurze Absätze, keine Überschriften.
- Beginne damit, warum genau dieses Unternehmen und diese Rolle, nicht mit "hiermit bewerbe ich mich".
- Nenne 2 bis 3 konkrete Punkte aus dem Lebenslauf, die zur Stelle passen.
- Ehrlich bleiben: keine Erfahrung erfinden, die nicht im Lebenslauf steht.
- Verfügbarkeit nennen: bis zu 20 Stunden pro Woche als Werkstudent.
- Sachliches, sicheres Deutsch, Sie-Form. Keine Floskeln wie "teamfähig" oder "motiviert".
- Schließe mit einem einfachen Satz, dass du dich über ein Gespräch freust.',
CURRENT_TIMESTAMP);
