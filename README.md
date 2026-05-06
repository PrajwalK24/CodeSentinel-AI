# CodeSentinel

CodeSentinel is a Spring Boot 3.x Java full stack web application for automated, rule-based code review and intelligent bug detection. It uses JSP, Bootstrap 5, Spring Security sessions, Spring Data JPA, MySQL 8, and a Java static analysis engine with no external ML libraries.

## Features

- Developer registration and session login
- BCrypt password hashing and role-based access control
- Admin and developer dashboards
- File upload and pasted-code analysis
- Rule-based detection for infinite loops, possible null pointer usage, nested loops, hardcoded credentials, and cyclomatic complexity
- Persistent submissions, reports, and line-by-line bug issues
- Dark developer-tool UI inspired by GitHub, VS Code, and SonarQube

## Seed Accounts

- Admin: `admin@codesentinel.com` / `Admin@1234`
- Developer: `dev@codesentinel.com` / `Dev@1234`

The accounts are inserted by a `CommandLineRunner` on first startup.

## Setup

1. Install Java 17, Maven, and MySQL 8.
2. Create the database:

```sql
CREATE DATABASE codesentinel_db;
```

3. Update MySQL credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=yourpassword
```

4. Run the app:

```bash
mvn spring-boot:run
```

5. Open:

```text
http://localhost:8080
```

Spring JPA is configured with `spring.jpa.hibernate.ddl-auto=update`, so tables are created automatically. A full manual schema is also available at `src/main/resources/schema.sql`.

## Routes

- `/login`
- `/register`
- `/dashboard`
- `/analyze`
- `/history`
- `/report/{id}`
- `/admin/dashboard`
- `/admin/users`
- `/admin/monitor`
- `/logout`

## Supported Upload Types

`.java`, `.py`, `.js`, `.cpp`, `.txt`
