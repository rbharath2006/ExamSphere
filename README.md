# ExamSphere — Online Examination System

ExamSphere is a full-stack online examination system for administering users, creating examinations, conducting timed student attempts, evaluating answers on the server, and publishing results under teacher control.

## Problem statement

Traditional examination workflows often require separate tools for account management, question authoring, timed delivery, evaluation, and result publication. ExamSphere provides these workflows in one authenticated application while keeping answer keys and unpublished scores on the server.

## Project objectives

- Provide role-specific workflows for administrators, teachers, and students.
- Support the complete examination lifecycle from draft creation to result publication.
- Enforce ownership, deadlines, evaluation, and result visibility on the backend.
- Provide a responsive React interface backed by session-authenticated APIs.
- Validate the database schema and application behavior with automated tests.

## Main features

### Administrator

- Dashboard statistics.
- Student and teacher listing with search, role filtering, and pagination.
- Teacher account provisioning.
- Account enable/disable management.

### Teacher

- Create and manage examinations.
- Create, edit, list, and delete questions and options.
- Publish and close examinations.
- Publish results only after an examination is closed, all attempts are complete, and calculated results exist.
- View publication state and timestamp.

### Student

- Browse available, upcoming, and past examinations.
- View examination instructions.
- Start and resume one attempt per examination.
- Select and save answers.
- Submit manually or be submitted automatically after the deadline.
- View results only after the teacher publishes them.

## Technology stack

- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS.
- **Backend:** Java 25, Spring Boot 4.1.2, Spring MVC.
- **Security:** Spring Security, session authentication, BCrypt password hashing, CSRF protection.
- **Persistence:** Spring Data JPA and Hibernate.
- **Database:** PostgreSQL 18.
- **Migrations:** Flyway.
- **Build tools:** Maven Wrapper and npm.

## System architecture

The frontend runs as a Vite development/build application and communicates with the Spring Boot backend over JSON HTTP APIs. The backend authenticates users with a database-backed HTTP session and applies role authorization to `/api/admin/**`, `/api/teacher/**`, and `/api/student/**`.

The backend owns examination state, attempt deadlines, answer validation, scoring, automatic submission, and result publication. PostgreSQL stores users, examinations, questions, options, attempts, answers, and calculated results. Flyway applies the database migrations before Hibernate validates the schema.

## Roles

| Role | Responsibilities |
|---|---|
| `ADMIN` | View dashboard statistics, manage users, and provision teachers. |
| `TEACHER` | Manage owned examinations and questions, publish/close examinations, and publish results. |
| `STUDENT` | Take examinations, save answers, submit attempts, and view published results. |

## Database overview

The migrations create and maintain:

- `users` for authenticated accounts and roles.
- `exams` for examination metadata, lifecycle status, ownership, schedules, totals, and `results_published_at`.
- `questions` and `question_options` for server-managed question content and answer keys.
- `exam_attempts` for one student attempt per examination, status, deadlines, and submission timestamps.
- `student_answers` for selected options.
- `exam_results` for one calculated result per attempt.

The V2 schema includes constraints for schedules, attempt expiry, submission status, answer ownership, result uniqueness, and score ranges. Do not edit applied migrations; create a new migration for future schema changes.

## Prerequisites

- Java 25 and `JAVA_HOME` configured.
- PostgreSQL 18 running locally or reachable through a configured JDBC URL.
- Node.js and npm.
- PowerShell on Windows for the commands below.

## PostgreSQL database setup

Create a database and application role using PostgreSQL administration tools. Do not place the password in source control:

```sql
CREATE USER exam_app WITH PASSWORD '<choose-a-local-password>';
CREATE DATABASE online_exam_db OWNER exam_app;
```

The default backend configuration is:

```text
DB_URL=jdbc:postgresql://localhost:5432/online_exam_db
DB_USERNAME=exam_app
DB_PASSWORD=<required>
```

The backend uses `ddl-auto: validate` and Flyway. On startup, Flyway applies the migrations and Hibernate validates the resulting schema. The repository does not provide production credentials or seed passwords.

## Backend installation and execution

From the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

To use a non-default database without editing configuration:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/online_exam_db"
$env:DB_USERNAME = "exam_app"
$env:DB_PASSWORD = "<your-local-password>"
.\mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8080`.

## Frontend installation and execution

From the repository root:

```powershell
cd frontend
npm install
npm run dev
```

The Vite development server normally runs at `http://localhost:5173`, which is the frontend origin allowed by the backend CORS configuration. The frontend API client currently targets `http://localhost:8080`.

For a production build:

```powershell
npm run build
```

## Testing

Run the backend test suite:

```powershell
cd backend
.\mvnw.cmd test
```

Run the frontend checks:

```powershell
cd frontend
npm run lint
npm run build
```

The verified project state includes 58 passing backend tests, 10 passing PostgreSQL integration tests, 9 passing HTTP security integration tests, and passing frontend lint and production build checks. Integration tests must use an isolated test database configured by the test environment; never point test writes at `online_exam_db`.

## Important API endpoints

All authenticated requests use the session established by `/api/auth/login`. Mutating requests require the CSRF token returned by `GET /api/csrf`.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Public health check. |
| `GET` | `/api/csrf` | Obtain the CSRF token/cookie. |
| `POST` | `/api/auth/register` | Register a student account. |
| `POST` | `/api/auth/login` | Session login using `email` and `password`. |
| `GET` | `/api/auth/me` | Retrieve the authenticated user. |
| `POST` | `/api/auth/logout` | Invalidate the session. |
| `GET` | `/api/admin/dashboard/summary` | Administrator dashboard statistics. |
| `GET` | `/api/admin/users` | List managed users. |
| `PATCH` | `/api/admin/users/{id}/status` | Enable or disable an account. |
| `POST` | `/api/admin/teachers` | Create a teacher account. |
| `GET` | `/api/teacher/exams` | List examinations owned by the teacher. |
| `POST` | `/api/teacher/exams` | Create a draft examination. |
| `POST` | `/api/teacher/exams/{examId}/publish` | Publish an examination. |
| `POST` | `/api/teacher/exams/{examId}/close` | Close an examination. |
| `POST` | `/api/teacher/exams/{examId}/results/publish` | Publish results for a closed examination. |
| `GET` | `/api/teacher/exams/{examId}/questions` | List owned examination questions. |
| `POST` | `/api/teacher/exams/{examId}/questions` | Add a question. |
| `PUT` | `/api/teacher/exams/{examId}/questions/{questionId}` | Update a question. |
| `DELETE` | `/api/teacher/exams/{examId}/questions/{questionId}` | Delete a question. |
| `GET` | `/api/student/exams` | List examinations visible to the student. |
| `POST` | `/api/student/exams/{examId}/attempts/start` | Start an attempt. |
| `GET` | `/api/student/attempts/{attemptId}` | Retrieve the student’s own attempt. |
| `PUT` | `/api/student/attempts/{attemptId}/answers` | Save an answer. |
| `POST` | `/api/student/attempts/{attemptId}/submit` | Submit an attempt. |
| `GET` | `/api/student/attempts/{attemptId}/result` | Retrieve a published result. |

The result endpoints return HTTP 409 while the owning teacher has not published the examination results. The backend, not the frontend, remains authoritative for all validation and access control.

## Security features

- Role-based authorization for administrator, teacher, and student API namespaces.
- Database-backed session authentication with BCrypt password hashing.
- CSRF protection for state-changing requests.
- Enabled-account checks on authenticated requests.
- Teacher ownership checks for examination and question operations.
- Student ownership checks for attempts and results.
- Server-derived deadlines and automatic submission.
- Server-side answer validation and evaluation.
- Student DTOs omit correctness flags and answer keys.
- Results remain unavailable until teacher-controlled publication.
- Transactional operations and pessimistic locking for relevant examination/attempt races.
- Database constraints for ownership relationships, schedules, status transitions, and result integrity.

## Known limitations and future enhancements

- Local configuration uses HTTP, localhost, and a fixed frontend API base URL; production should provide HTTPS, secure cookies, environment-specific origins, and environment-provided API configuration.
- No production deployment manifests or profile are included.
- Login and registration rate limiting must be provided by a reverse proxy, gateway, or distributed limiter before internet exposure.
- The frontend currently assumes the backend is available at `http://localhost:8080`.
- Future work could add richer teacher result summaries, automated deployment configuration, observability, rate limiting, and broader browser-level end-to-end testing.
