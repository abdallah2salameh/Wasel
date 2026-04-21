# Wasel Palestine Backend

Wasel Palestine is an API-centric smart mobility backend focused on Palestinian daily movement intelligence. This project provides versioned REST APIs for checkpoints, incidents, crowdsourced reports, route estimation, subscriptions, and alerts, backed by a relational database and secured with JWT access and refresh tokens.

## System Overview

Core capabilities:

- Checkpoint and road incident registry with status history
- Crowdsourced mobility disruption reports with duplicate detection and moderation audit trail
- Heuristic route estimation with checkpoint, incident, and weather-aware adjustments
- Regional alert subscriptions and generated alert records
- External integration with OpenStreetMap/OSRM routing-geocoding and OpenWeather weather APIs
- Versioned API design under `/api/v1/...`

## Tech Stack

- Java 21
- Spring Boot 3.2
- Spring Web, Validation, Security, Data JPA
- PostgreSQL + Flyway
- JWT via `jjwt`
- OpenAPI/Swagger UI
- Docker + Docker Compose
- k6 scripts for load testing

## Architecture

```text
Clients
  -> REST API (/api/v1/*)
     -> Security layer (JWT access/refresh)
     -> Application services
        -> JPA repositories (ORM)
        -> JdbcTemplate query repository (raw SQL analytics)
        -> External integrations
           -> OSRM / Nominatim
           -> OpenWeather
     -> PostgreSQL
     -> Flyway migrations
```

## Modules

- `security`: authentication, authorization, JWT issuance, refresh token storage, seeded admin
- `mobility.checkpoint`: checkpoint registry and status history
- `mobility.incident`: incident lifecycle, filtering, verification, closure, analytics
- `mobility.report`: crowdsourced submission, voting, moderation, auditability
- `mobility.alert`: subscriptions and generated alert records
- `mobility.route`: route estimation heuristics and geocoding
- `integration`: external API clients, outbound rate limiting, timeout-safe HTTP clients

## Database Schema

Main entities:

- `app_users`
- `refresh_tokens`
- `checkpoints`
- `checkpoint_status_history`
- `incidents`
- `crowd_reports`
- `report_votes`
- `moderation_audit`
- `alert_subscriptions`
- `alert_records`

ERD summary:

```text
app_users 1---* refresh_tokens
app_users 1---* incidents(created_by)
app_users 1---* crowd_reports(submitted_by)
app_users 1---* crowd_reports(reviewed_by)
app_users 1---* report_votes
app_users 1---* moderation_audit
app_users 1---* alert_subscriptions

checkpoints 1---* checkpoint_status_history
checkpoints 1---* incidents

crowd_reports 1---* report_votes
alert_subscriptions 1---* alert_records
incidents 1---* alert_records
```

Schema is defined in [V1__init_schema.sql](/C:/Users/AbdallahSalameh/IdeaProjects/demo2/src/main/resources/db/migration/V1__init_schema.sql).

## API Design Rationale

- All business endpoints are versioned with `/api/v1`
- Public read endpoints are open for incident/checkpoint discovery and route estimation
- Mutation endpoints use role-based access control for moderators/admins where required
- Pagination is standardized through Spring Data page responses
- Error responses are centralized through a common JSON error format
- Both ORM and raw SQL are used:
  - ORM for transactional domain workflows
  - Raw SQL via `IncidentQueryRepository` for analytics aggregation

## Main Endpoints

Authentication:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

Mobility registry:

- `GET /api/v1/incidents`
- `POST /api/v1/incidents`
- `POST /api/v1/incidents/{id}/verify`
- `POST /api/v1/incidents/{id}/close`
- `GET /api/v1/incidents/analytics/summary`
- `GET /api/v1/checkpoints`
- `POST /api/v1/checkpoints`
- `PATCH /api/v1/checkpoints/{id}/status`

Crowdsourcing:

- `POST /api/v1/reports`
- `GET /api/v1/reports`
- `POST /api/v1/reports/{id}/vote`
- `POST /api/v1/reports/{id}/moderate`

Routing and alerts:

- `POST /api/v1/routes/estimate`
- `GET /api/v1/routes/geocode`
- `POST /api/v1/alerts/subscriptions`
- `GET /api/v1/alerts/records`

OpenAPI is available at `/v3/api-docs`, and Swagger UI at `/swagger-ui`.

## External Integrations

Routing and geocoding:

- OSRM routing endpoint
- Nominatim geocoding endpoint
- Caching applied to route and geocoding results
- Timeout-safe clients and outbound rate limiting included

Weather:

- OpenWeather current weather lookup
- Optional via `WEATHER_ENABLED` and `WEATHER_API_KEY`
- Cached responses and outbound rate limiting included

## Abuse Prevention and Moderation

- Validation constraints on report payloads
- Submission throttling per authenticated user or client fingerprint
- Duplicate detection based on recent nearby reports of the same category
- Voting-driven confidence score
- All moderation actions persisted in `moderation_audit`

## Route Estimation Strategy

Route estimation combines:

- external provider base route if available
- heuristic fallback when the provider is unavailable
- delay adjustments for verified incidents
- optional checkpoint avoidance detours
- optional avoided-area detours
- weather-based slowdowns when enabled

This is intentionally heuristic and explainable rather than claiming exact navigation accuracy.

## Frontend

A full web frontend now lives under [frontend](/C:/Users/AbdallahSalameh/IdeaProjects/demo2/frontend). It is built with Next.js App Router and wired directly to the Spring Boot API.

Frontend pages:

- `/` overview dashboard
- `/incidents` incident browser
- `/checkpoints` checkpoint browser
- `/routes` route estimator
- `/reports` report submission and moderation queue
- `/alerts` subscription and alert record management
- `/auth` JWT login and registration

Frontend environment:

- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`

The backend now enables CORS for the configured frontend origin so browser requests from `http://localhost:3000` are accepted.

## Running Locally

### Maven

```bash
./mvnw spring-boot:run
```

### Docker

```bash
docker compose up --build
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend default URL:

- `http://localhost:3000`

Backend default URL:

- `http://localhost:8080`

Default local credentials:

- Admin email: `admin@wasel.local`
- Admin password: `ChangeMe123!`

Change them through environment variables before using this outside local development.

## Testing Strategy

Implemented:

- Spring Boot context test
- MockMvc smoke tests for auth registration and public report submission
- H2 test profile with Flyway migrations enabled

Run:

```bash
./mvnw test
```

## Performance Testing

k6 scripts are included under [k6](/C:/Users/AbdallahSalameh/IdeaProjects/demo2/k6):

- `incidents-read.js`
- `reports-write.js`
- `mixed.js`
- `spike.js`
- `soak.js`

Example:

```bash
k6 run -e BASE_URL=http://localhost:8080 k6/incidents-read.js
```

A reporting template is provided in [performance-report-template.md](/C:/Users/AbdallahSalameh/IdeaProjects/demo2/docs/performance-report-template.md).

## API-Dog

API-Dog guidance is documented in [apidog/README.md](/C:/Users/AbdallahSalameh/IdeaProjects/demo2/apidog/README.md). The intended workflow is to import the generated OpenAPI document from `/v3/api-docs` into API-Dog and build the required collections/environments from there.

## Current Scope

Implemented in this repository:

- mandatory relational persistence
- versioned REST API foundation
- JWT access and refresh token flow
- Docker deployment assets
- two external integration clients
- baseline testing and load-test scaffolding

Still expected as project follow-up work:

- populate realistic seed/reference data
- generate API-Dog export files from the running API
- execute and document k6 benchmark runs with measured results
- tighten production-grade observability, notification delivery, and moderation policy rules
