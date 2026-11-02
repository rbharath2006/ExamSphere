# PostgreSQL Integration Test Report

## Scope

`backend/src/test/java/com/onlineexam/backend/integration/PostgreSqlExamWorkflowIntegrationTest.java`
was added as a real Spring Boot integration test. It uses the `test` profile,
the `exam_test_app` role, Spring Data repositories, real service beans, real
transactions, and PostgreSQL row locks. It does not use Mockito for database or
transaction behavior.

Files changed for this correction:

- `backend/src/test/java/com/onlineexam/backend/integration/PostgreSqlExamWorkflowIntegrationTest.java`
- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java`
- `backend/src/main/java/com/onlineexam/backend/BackendApplication.java`
- `docs/POSTGRES_INTEGRATION_TEST_REPORT.md`

The production changes only inject the standard UTC `Clock`, preserving the
existing service behavior while allowing the integration test to deterministically
exercise the post-lock deadline transition.

Every test verifies `SELECT current_database()` is
`online_exam_test_db` before creating fixtures. Fixtures use unique
run-scoped email/title values. Cleanup deletes only tracked fixture rows in
foreign-key-safe order, scoped by each tracked exam ID; it does not truncate
tables or run Flyway clean. Cleanup verifies the database identity again before
deleting anything.

## Scenarios covered

- `REQUIRES_NEW` expiration commits an expired attempt and result, survives an
  outer rollback, and remains idempotent.
- Two concurrent starts for one student/examination preserve the unique attempt
  constraint and produce at most one conflict.
- Concurrent submission and expiration produce one terminal attempt and one
  result.
- An answer request blocked on a real attempt row lock is rejected after the
  persisted deadline has passed.
- A deterministic test clock drives the post-lock deadline branch after the
  pre-lock expiration check returns, while the service still uses real
  PostgreSQL transactions and row locks.
- Closed, submitted, and auto-submitted attempts reject answer changes.
- Student attempt and result ownership is enforced.
- Teacher ownership and published-question immutability are enforced.
- PostgreSQL rejects duplicate attempts, duplicate results, invalid foreign
  keys, cross-question selected options, and out-of-range result marks.

## Corrections in this iteration

- Expired attempts are inserted with valid future expiry values, then
  `started_at` and `expires_at` are backdated together using parameterized SQL.
- The manual-submission fixture resets the adjustable test clock after JPA
  persistence so `submitted_at` cannot precede `started_at`.
- Closed, submitted, and auto-submitted answer attempts assert HTTP 409.
  The post-lock deadline test asserts the service boolean `false`, then
  verifies committed auto-submission, timestamp, one result, and no answer.
- Cleanup is scoped by tracked exam IDs and covers attempts created by
  concurrent `start` calls.

## Execution status

The test class was compiled successfully with:

```text
backend\mvnw.cmd -DskipTests test-compile
```

In this execution environment, `TEST_DB_PASSWORD` is not available. The
targeted command therefore failed during Spring/Flyway startup with password
authentication failure for `exam_test_app`, before any fixture write. It did
not fall back to `online_exam_db`, and no integration test result is claimed as
passed.

Latest targeted command counts:

- `PostgreSqlExamWorkflowIntegrationTest`: 9 errors, 0 failures, 0 skipped.
- All errors occurred during isolated PostgreSQL authentication startup; no
  test method wrote fixture data.
- The required full backend and frontend commands were not run because the
  targeted integration suite did not pass.

The corrected test source also compiles successfully with
`backend\mvnw.cmd -DskipTests test-compile`.

The required remaining commands are dependent on supplying the isolated test
database password:

```text
backend\mvnw.cmd clean test
frontend: npm run lint
frontend: npm run build
```

## Limitations

The suite does not claim coverage until it runs successfully against the
isolated PostgreSQL database. The concurrency tests use real concurrent Spring
transactions and database locks, but execution requires valid credentials and
the test database to be available.
