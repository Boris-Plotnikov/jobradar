UPDATE prompt_templates
SET updated_at = CURRENT_TIMESTAMP,
    body = REPLACE(REPLACE(body,
        'Prefer evidence that can be checked through a link.',
        'Prefer evidence that can be checked through a link. Use only the details that matter for this employer and leave out the rest, for example how the AI costs of JobRadar are controlled when the job has nothing to do with AI. Describe at most two projects in detail.'),
        'one or two numbers or details are better than adjectives.',
        'one or two numbers or details are better than adjectives. Connect each piece of evidence to a task or requirement of the vacancy in its own words, for example: code reviews and tests -> 39 automated tests and CI on every push. The paragraph must read as an answer to the vacancy, not as a list of technologies.')
WHERE code = 'COVER_LETTER_EN';

UPDATE prompt_templates
SET updated_at = CURRENT_TIMESTAMP,
    body = REPLACE(REPLACE(body,
        'Bevorzuge Nachweise, die sich über einen Link prüfen lassen.',
        'Bevorzuge Nachweise, die sich über einen Link prüfen lassen. Verwende nur Details, die für diesen Arbeitgeber zählen, und lass den Rest weg, zum Beispiel wie die KI-Kosten von JobRadar begrenzt werden, wenn die Stelle nichts mit KI zu tun hat. Beschreibe höchstens zwei Projekte im Detail.'),
        'ein oder zwei Zahlen oder Details sind besser als Adjektive.',
        'ein oder zwei Zahlen oder Details sind besser als Adjektive. Verbinde jeden Nachweis mit einer Aufgabe oder Anforderung der Anzeige in deren Worten, zum Beispiel: Code-Reviews und Tests -> 39 automatisierte Tests und CI bei jedem Push. Der Absatz muss sich wie eine Antwort auf die Anzeige lesen, nicht wie eine Liste von Technologien.')
WHERE code = 'COVER_LETTER_DE';
