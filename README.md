# FeatureForge

Distributed feature flag platform. Java 21 / Spring Boot 3 backend with
PostgreSQL (system of record), MongoDB (flag/audit documents, Day 2+),
Redis (evaluation cache, Day 3+), Kafka (event stream, Day 4+), Angular
dashboard (Day 3+), Docker + Kubernetes deployment (Day 4-5).

## Day 1 — done

- Project skeleton (Maven, package-by-layer: `domain`, `dto`, `repository`,
  `service`, `controller`, `config`, `security`, `exception`)
- JWT authentication: register, login, stateless filter chain
- PostgreSQL: `users` table via Flyway migration, Spring Data JPA
- MongoDB: connection + repository scanning wired up (no documents yet —
  those land Day 2 with flag definitions and audit events)
- Docker: multi-stage `Dockerfile`, full-stack `docker-compose.yml`
  (Postgres, Mongo, Redis, Kafka+Zookeeper, app)
- Global exception handling with structured JSON error responses
- Unit tests for `AuthService` (register success/conflict, login success/failure)

## Design decisions

- **BCrypt strength 12** — deliberate cost above the Spring default (10) since
  this is an auth service, not a high-throughput hot path.
- **JWT over sessions** — stateless auth fits a service that will eventually
  sit behind Kubernetes with multiple replicas; no shared session store needed
  for auth itself (Redis is reserved for flag-evaluation caching, not sessions).
- **Access + refresh token pair** — 15 min access / 7 day refresh. Refresh
  rotation/blacklisting is a Day 5 hardening item, not in scope for Day 1.
- **Flyway over `ddl-auto: update`** — `ddl-auto: validate` in
  `application.yml` means schema drift fails loudly instead of Hibernate
  silently altering tables in "prod-like" environments.
- **DAO auth provider, not custom `AuthenticationProvider`** — Spring's
  `DaoAuthenticationProvider` + `UserDetailsService` is the standard integration
  point; no reason to hand-roll credential checking.
- **`open-in-view: false`** — forces services to fetch what they need inside
  the transaction boundary rather than lazy-loading in the controller/view
  layer, which is the usual source of `LazyInitializationException` and
  N+1 queries in Spring apps.
- **MongoDB wired but empty on Day 1** — the relational schema (users, and
  organizations/projects coming Day 2) needs to be settled before document
  shapes for flags/audit events are locked in, so Mongo setup is connection
  + config only this session.

## Running locally

```bash
docker compose up -d postgres mongo   # infra only
./mvnw spring-boot:run                # app on :8080
```

or full stack in containers:

```bash
docker compose up --build
```

## API surface (Day 1)

| Method | Path                  | Auth | Description              |
|--------|-----------------------|------|---------------------------|
| POST   | `/api/v1/auth/register` | No   | Create account, returns JWT pair |
| POST   | `/api/v1/auth/login`    | No   | Authenticate, returns JWT pair   |
| GET    | `/api/v1/users/me`      | Yes  | Return current authenticated user |

## Next (Day 2)

Organizations & Projects CRUD, Feature Flags CRUD, percentage-rollout engine.
