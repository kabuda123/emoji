# emoji-server

Backend monolith for the iOS AI emoji generation MVP.

## Stack
- Java 21
- Spring Boot 3.5.x
- PostgreSQL
- Redis
- Flyway

## Modules
- `auth`
- `user`
- `template`
- `generation`
- `payment`
- `media`
- `audit`
- `admin`
- `common`

## Local run
1. Start infrastructure with `docker/docker-compose.yml`.
   If `5432` or `6379` is already used locally, override them with `HOST_DB_PORT` or `HOST_REDIS_PORT`.
2. Use the Maven wrapper to start the app.
3. Set `SPRING_PROFILES_ACTIVE=local` for local development.

## Notes
- The project keeps a monolith-first structure with explicit module boundaries.
- Third-party AI providers must be accessed through a provider adapter, not from controller code.
- The repo only contains initialization scaffolding in this commit.
