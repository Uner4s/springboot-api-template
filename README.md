# springboot-api-template

Production-ready Spring Boot 3 API template with a strict layered architecture, JWT authentication, and granular permission-based authorization. Built to serve as a starting point for REST APIs — the same patterns used across NestJS, .NET, and Django equivalents in this project family.

## Stack

| Technology                  | Purpose                       | Version     |
| --------------------------- | ----------------------------- | ----------- |
| Spring Boot                 | Backend framework             | 3.x         |
| Java                        | Language                      | 21 (LTS)    |
| Spring Data JPA + Hibernate | ORM                           | see pom.xml |
| Spring Data MongoDB         | Document store (optional)     | see pom.xml |
| Spring Security             | Auth and authorization        | see pom.xml |
| JJWT                        | JWT generation and validation | 0.12.6      |
| MapStruct                   | Entity ↔ DTO mapping          | 1.6.3       |
| Lombok                      | Boilerplate reduction         | see pom.xml |
| Flyway                      | Schema migrations             | see pom.xml |
| H2                          | File-based database (dev)     | see pom.xml |
| dotenv-java                 | `.env` loading                | 3.0.0       |

## Features

- **Layered architecture** — strict Controller → Service → Repository separation
- **JWT authentication** — stateless, token carries full permission set
- **Granular permissions** — `AppPermission` enum + `@PreAuthorize` on every endpoint
- **Standardized responses** — every endpoint returns `ApiResponse<T>` with the same shape
- **Global error handling** — `GlobalExceptionHandler` catches everything; no try/catch in controllers
- **MapStruct mapping** — compile-time entity ↔ DTO mapping, no manual field assignment
- **Flyway migrations** — versioned SQL schema; `ddl-auto: validate` always
- **Soft delete** — `isDeleted` flag on all entities, never hard deletes
- **H2 for dev** — file-based database, no external DB required to run locally
- **MongoDB ready** — activates automatically when `MONGODB_URI` is set

## Getting started

**Prerequisites:** Java 21, Maven (wrapper included)

```bash
# 1. Clone the repo
git clone https://github.com/your-username/springboot-api-template.git
cd springboot-api-template

# 2. Create the .env file at the project root
cp .env.example .env   # then edit with your values

# 3. Run
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway runs automatically and applies `V1__init.sql` on first boot.

## Environment variables

Create a `.env` file at the project root (never commit this file):

```env
JWT_SECRET=your-secret-key-at-least-32-characters-long
JWT_EXPIRATION_MS=86400000
```

For PostgreSQL (production):

```env
DB_URL=jdbc:postgresql://localhost:5432/your_db
DB_USER=postgres
DB_PASSWORD=your_password
```

For MongoDB (when needed):

```env
MONGODB_URI=mongodb://localhost:27017
MONGODB_DATABASE=your_db
```

## H2 dev console

While running locally, the H2 web console is available at `http://localhost:8080/h2-console`.

```
JDBC URL:  jdbc:h2:file:./data/backend_db
Username:  sa
Password:  (leave blank)
```

## API endpoints

### Auth

| Method | Path                 | Auth   | Description         |
| ------ | -------------------- | ------ | ------------------- |
| `POST` | `/api/v1/auth/login` | Public | Returns a JWT token |

### Users

| Method   | Path                  | Permission     | Description      |
| -------- | --------------------- | -------------- | ---------------- |
| `GET`    | `/api/v1/users`       | `READ_USERS`   | List all users   |
| `GET`    | `/api/v1/users/{id}`  | `READ_USERS`   | Get user by ID   |
| `POST`   | `/api/v1/users`       | `MANAGE_USERS` | Create user      |
| `PATCH`  | `/api/v1/users/{id}`  | `MANAGE_USERS` | Update user      |
| `DELETE` | `/api/v1/users/{id}`  | `MANAGE_USERS` | Soft delete user |

All responses follow this shape:

```json
{
  "success": true,
  "data": {},
  "message": "Action completed",
  "error": null
}
```

## Project structure

```
src/main/java/com/example/backend/
├── BackendApplication.java
├── config/               Spring beans — Security, JPA, Mongo, CORS
├── controllers/          Routing and I/O only — no business logic
├── services/             All business logic lives here
├── repositories/         Spring Data interfaces
│   └── mongo/            MongoDB repositories (activated with MONGODB_URI)
├── entities/             JPA entities — extend BaseEntity
├── documents/            MongoDB documents — extend BaseDocument
├── dtos/
│   ├── request/          Input DTOs with Bean Validation
│   └── response/         Output DTOs + ApiResponse<T>
├── mappers/              MapStruct interfaces
├── security/             JWT filter, service, UserDetails
│   └── permissions/      AppPermission enum + evaluator
├── exceptions/           ApiException + GlobalExceptionHandler
└── helpers/              Pure static utilities
```

## Switching to PostgreSQL

1. Replace the datasource block in `application.yaml`:

```yaml
datasource:
  url: ${DB_URL}
  username: ${DB_USER}
  password: ${DB_PASSWORD}
  driver-class-name: org.postgresql.Driver
jpa:
  properties:
    hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialect
```

2. Remove the `h2.console` block and the `autoconfigure.exclude` entries.
3. Add `DB_URL`, `DB_USER`, and `DB_PASSWORD` to your `.env`.

## Permissions

Permissions are defined in `AppPermission.java`. A `Role` holds a set of permissions. The JWT embeds them at login — no DB lookup per request. `SUPER_ADMIN` bypasses all permission checks.

```
READ_USERS · MANAGE_USERS
READ_ROLES · MANAGE_ROLES
READ_AUDITS · MANAGE_AUDITS
READ_REPORTS · EXPORT_REPORTS
SUPER_ADMIN
```

Protect any endpoint with:

```java
@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'PERMISSION_NAME')")
```
