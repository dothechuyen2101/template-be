# Reusable Spring Boot Backend Template

A production-oriented Java 21 backend starter for REST APIs. It includes JWT authentication, role-based authorization, PostgreSQL, Flyway, standardized responses, validation, OpenAPI, auditing, tests, Docker, and environment-specific configuration.

## Technology stack

- Java 21 and Maven
- Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation
- JWT access tokens with hashed, rotating opaque refresh tokens
- PostgreSQL for development/production and H2 for automated tests
- Flyway database migrations
- springdoc-openapi 3.0.3 and Swagger UI
- Lombok and MapStruct
- Actuator, SLF4J logging, and request correlation IDs
- JUnit, Mockito, MockMvc, and Testcontainers
- Docker and Docker Compose

## Architecture

The project uses a modular layered architecture. Controllers are thin, services own business logic, repositories only handle persistence, and API entities are never returned directly.

```text
src/main/java/com/example/template
├── common
│   ├── audit
│   ├── config
│   ├── constants
│   ├── exception
│   ├── response
│   ├── security
│   └── util
├── modules
│   ├── auth
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   ├── role
│   │   ├── entity
│   │   └── repository
│   ├── user
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── mapper
│   │   ├── repository
│   │   └── service
│   └── publicapi
└── TemplateApplication.java
```

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker and Docker Compose for PostgreSQL or the complete containerized stack

## Run locally

Start only PostgreSQL:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

The `dev` profile is active by default and uses:

- Database: `jdbc:postgresql://localhost:5432/template_db`
- Username: `template_user`
- Password: `template_password`
- Server: `http://localhost:8080`

Override any value with the environment variables listed below. The application validates the schema against the Flyway-managed database at startup.

## Run with Docker Compose

Create a local environment file and replace its secrets:

```bash
cp .env.example .env
openssl rand -base64 64
docker compose up --build
```

Put the generated value in `JWT_SECRET` before using the stack outside throwaway local development.

Stop the stack:

```bash
docker compose down
```

Delete the local database volume as well:

```bash
docker compose down -v
```

## Profiles

- `dev`: PostgreSQL with local defaults and verbose application logging.
- `test`: H2 in PostgreSQL compatibility mode; used by unit/API integration tests.
- `prod`: requires database credentials, JWT secret, and CORS origins from the environment. Swagger is disabled unless explicitly enabled.

Select a profile with:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/template-backend-0.0.1-SNAPSHOT.jar
```

## Environment variables

| Variable | Purpose | Local default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `SERVER_PORT` | HTTP port | `8080` |
| `DB_URL` | JDBC URL | Local PostgreSQL URL |
| `DB_USERNAME` | Database username | `template_user` |
| `DB_PASSWORD` | Database password | `template_password` |
| `DB_POOL_SIZE` | Maximum Hikari pool size | Profile-specific |
| `JWT_SECRET` | Base64 HMAC key, at least 32 decoded bytes | Development-only key |
| `JWT_ACCESS_EXPIRATION` | Access-token lifetime | `15m` |
| `JWT_REFRESH_EXPIRATION` | Refresh-token lifetime | `7d` |
| `JWT_ISSUER` | JWT issuer | `template-backend` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Local frontend origins |
| `SWAGGER_ENABLED` | Enable Swagger in production | `false` |
| `ADMIN_BOOTSTRAP_ENABLED` | Create an initial administrator | `false` |
| `ADMIN_USERNAME` | Bootstrap administrator username | Empty |
| `ADMIN_EMAIL` | Bootstrap administrator email | Empty |
| `ADMIN_PASSWORD` | Bootstrap administrator password | Empty |
| `ADMIN_FULL_NAME` | Bootstrap administrator name | `Administrator` |

Never commit a real `.env` file or production secret.

## Initial administrator

There is deliberately no hard-coded default account. To create an administrator on an empty database, set:

```bash
export ADMIN_BOOTSTRAP_ENABLED=true
export ADMIN_USERNAME=admin
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD='use-a-strong-password'
export ADMIN_FULL_NAME='Administrator'
```

Start the application once, confirm the account exists, then disable bootstrap. The process is idempotent for an existing username/email.

## API documentation and health

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Public example: `http://localhost:8080/api/v1/public/ping`

Use Swagger's **Authorize** button with an access token. Do not include the `Bearer ` prefix in the token field.

## Main endpoints

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Public |
| `POST` | `/api/v1/auth/login` | Public |
| `POST` | `/api/v1/auth/refresh-token` | Public, valid refresh token required |
| `POST` | `/api/v1/auth/logout` | Public, revokes supplied refresh token |
| `GET` | `/api/v1/users/me` | Authenticated |
| `GET` | `/api/v1/admin/users` | ADMIN |
| `GET` | `/api/v1/admin/users/{id}` | ADMIN |
| `POST` | `/api/v1/admin/users` | ADMIN |
| `PATCH` | `/api/v1/admin/users/{id}` | ADMIN |
| `DELETE` | `/api/v1/admin/users/{id}` | ADMIN |

User listing accepts Spring pagination parameters:

```text
GET /api/v1/admin/users?page=0&size=20&sort=createdAt,desc&keyword=alice
```

Page size is capped at 100.

## Authentication example

Register:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123",
    "fullName": "Alice Example"
  }'
```

Call a protected endpoint:

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

Refresh tokens are random opaque values. Only their SHA-256 hashes are stored. Refreshing rotates and revokes the previous token; logout revokes the supplied token.

## Standard API response

Successful response:

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "errors": null,
  "timestamp": "2026-06-25T03:00:00Z"
}
```

Validation error:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "timestamp": "2026-06-25T03:00:00Z"
}
```

Stack traces and internal exception details are never returned to clients. Every response includes `X-Correlation-ID`; a safe client-supplied value is preserved, otherwise the server generates one.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration`. `V1__create_initial_schema.sql` creates:

- `users`
- `roles`
- `user_roles`
- `refresh_tokens`
- Default `ADMIN` and `USER` roles

Add schema changes as new immutable versioned migrations, for example:

```text
V2__add_user_avatar.sql
V3__create_orders.sql
```

Do not edit an already-applied production migration.

## Testing and build

Run all tests:

```bash
mvn test
```

Create the executable jar:

```bash
mvn clean package
java -jar target/template-backend-0.0.1-SNAPSHOT.jar
```

The suite includes:

- Mockito unit tests for authentication and user services
- MockMvc integration tests for registration, login, validation, JWT-protected access, and authorization
- A PostgreSQL Testcontainers startup/migration test, automatically skipped when Docker is unavailable

## Code conventions

- Keep controllers focused on HTTP concerns.
- Put transactions and business rules in services.
- Keep database queries in repositories.
- Use separate request and response DTOs.
- Use MapStruct for entity-to-response conversion.
- Never expose entities, passwords, tokens, or stack traces.
- Add validation at API boundaries and enforce invariants again in services/database constraints.
- Add new shared concerns under `common`; add business capabilities as modules.
- Log identifiers and outcomes, never passwords, access tokens, refresh tokens, or secrets.

## Reuse for a new project

1. Change `groupId`, `artifactId`, application name, and the base Java package.
2. Rename `TemplateApplication`.
3. Replace API metadata in `OpenApiConfig`.
4. Set a project-specific JWT issuer and secret-management strategy.
5. Add new modules under `modules`, with their own controller/DTO/entity/repository/service packages.
6. Add Flyway migrations instead of using Hibernate schema generation.
7. Review public paths, CORS origins, Actuator exposure, token lifetimes, and bootstrap behavior.
8. Replace local Docker credentials before deployment.

The default design favors explicit, familiar Spring patterns over framework-heavy abstractions, making the template easy to remove from as well as add to.
