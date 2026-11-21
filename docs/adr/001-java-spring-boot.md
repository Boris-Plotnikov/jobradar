# ADR-001: Java 21 and Spring Boot 3

- **Status:** Accepted
- **Date:** 2026-11-18

## Context

The service runs every day, calls several HTTP APIs, stores data in a relational database and has a REST API for a web UI. I study Java at university and want to work as a junior Java developer in Austria, where Spring is the most common backend framework.

## Options considered

| Option | Pros | Cons |
|---|---|---|
| Java + Spring Boot | Scheduling, REST, JPA, security, validation out of the box; most requested in job ads | More memory than a script |
| Plain Java + JDBC | Minimal | Writing HTTP server, mapping and security by hand |
| Python script + cron | Fast to write | Not my target stack; UI and security would be separate |

## Decision

Java 21 with Spring Boot 3: Spring Web, Spring Data JPA, Spring Security, Bean Validation, `@Scheduled`. Plain classes and records, constructor injection, no Lombok - every line is explicit.

## Consequences

- One JAR contains API, scheduler and the built UI.
- Memory ~250 MB with `-Xmx192m` - fits a small VPS together with other apps.
