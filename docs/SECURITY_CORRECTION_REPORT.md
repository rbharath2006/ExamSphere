# ExamSphere Security Correction Report

## Scope and starting state

Audit date: 2026-11-02

Branch: `feature/teacher-exam-management-ui`

Starting worktree state:

- 11 tracked files modified.
- 16 untracked files.
- 0 staged files and 0 conflicted files.
- No files were reset, cleaned, checked out, committed, pushed, or merged.

The complete starting diff and all listed source paths were inspected before changes.

## Focused correction on 2026-11-02

This follow-up preserved the earlier security corrections and modified only:

- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java`
- `backend/src/main/java/com/onlineexam/backend/repository/ExamAttemptRepository.java`
- `backend/src/main/java/com/onlineexam/backend/service/ExamAttemptEvaluationService.java`
- `backend/src/main/java/com/onlineexam/backend/service/ExpiredAttemptService.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentAttemptResponse.java`
- `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java`
- `frontend/src/services/authService.ts`
- `frontend/src/pages/TakeExamPage.tsx`
- `docs/SECURITY_CORRECTION_REPORT.md`

### Expiration transaction correction

Previously, `expireIfNeeded` changed the attempt and created the result inside the
outer `start`, `saveAnswer`, `getAttempt`, `submit`, or `result` transaction.
Those methods then raised a conflict for an expired attempt, which could roll
back the expiration and result.

Expiration now runs through `ExpiredAttemptService.expireIfNeeded`, annotated
with `@Transactional(propagation = REQUIRES_NEW)`. It rechecks ownership using
the locked `(attemptId, studentId)` lookup, changes the attempt to
`AUTO_SUBMITTED`, evaluates through the shared
`ExamAttemptEvaluationService`, and commits before the caller re-locks and
revalidates the attempt. Expiration is attempted before the caller acquires its
outer attempt lock, avoiding a suspended-transaction self-deadlock. Result
creation remains guarded by `existsByAttemptId`, so it occurs at most once.
The outer service returns a committed auto-submitted state for retrieval and
submission requests; answer saves report the conflict only after the committed
expiration transaction completes.

The follow-up concurrency correction removes managed `ExamAttempt` discovery
from the outer transaction. `ExamAttemptRepository` now provides scalar
ownership projections for attempt ID by exam/student and exam ID by
attempt/student. `start`, `getAttempt`, `saveAnswer`, `submit`, and `result`
use those projections before expiration, then load the attempt through a fresh
ownership-scoped pessimistic query after the `REQUIRES_NEW` transaction
completes. This prevents a stale persistence-context entity from being used
after a concurrent submit or auto-submit. No `EntityManager.refresh` is used.

Focused tests cover a submitted attempt returned by the fresh lock being
rejected for answer changes, already-submitted attempts being ignored by
expiration, expiration committing once, and the fresh scalar/locking path.

### Final deadline race correction

The outer request now performs a second deadline check after obtaining its
fresh ownership-scoped attempt lock. This closes the race where an attempt
could expire while the request was waiting for that lock. If an
`IN_PROGRESS` attempt is now past its persisted deadline, the locked entity is
changed to `AUTO_SUBMITTED`, saved, and evaluated exactly once in the current
transaction. The helper does not call `REQUIRES_NEW` while holding the lock,
and callers return a committed outcome rather than throwing an exception that
would roll back the transition. Late answer saves return a conflict only after
the service transaction commits; late submission and retrieval return the
auto-submitted result/state. Already submitted attempts are left unchanged.

### Server-derived answer state

`StudentAttemptResponse.answersAllowed` is calculated by the backend from:

- examination status is `PUBLISHED`;
- attempt status is `IN_PROGRESS`; and
- the current instant is before the persisted attempt deadline.

`TakeExamPage` redirects submitted and auto-submitted attempts to the result
page. For a closed examination, it displays a read-only message, disables
answer options using the server-derived flag, and still allows submission of
previously saved answers.

## Previous Copilot change ledger

All 12 files listed in the handoff were already present in the starting worktree. They were not attributed to this correction task:

1. `backend/src/main/java/com/onlineexam/backend/repository/ExamRepository.java` - prior exam repository queries and the teacher-owned pessimistic-lock query.
2. `backend/src/main/java/com/onlineexam/backend/repository/ExamAttemptRepository.java` - prior attempt lookup and pessimistic-lock queries.
3. `backend/src/main/java/com/onlineexam/backend/service/ExamService.java` - prior teacher exam listing, publishing, closing, and publish validation.
4. `backend/src/main/java/com/onlineexam/backend/service/QuestionService.java` - prior teacher question CRUD and draft/attempt checks.
5. `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java` - prior student exam, attempt, answer, timing, evaluation, and scheduling logic.
6. `backend/src/main/java/com/onlineexam/backend/dto/StudentExamResponse.java` - prior student exam listing DTO.
7. `backend/src/test/java/com/onlineexam/backend/service/ExamServiceTest.java` - prior teacher exam service tests.
8. `backend/src/test/java/com/onlineexam/backend/service/QuestionServiceTest.java` - prior question service tests.
9. `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java` - prior student service tests.
10. `frontend/src/services/authService.ts` - prior teacher and student API client functions.
11. `frontend/src/pages/StudentExamsPage.tsx` - prior student exam listing UI.
12. `frontend/src/pages/ExamInstructionsPage.tsx` - prior instructions/start UI.

The remaining existing modified/untracked files in the starting tree were also preserved.

## Corrections made in this task

### 1. Read-only question listing no longer requests a write lock

Files and final locations:

- `backend/src/main/java/com/onlineexam/backend/repository/ExamRepository.java:18-38`
- `backend/src/main/java/com/onlineexam/backend/service/QuestionService.java:110-118, 276-298`
- `backend/src/test/java/com/onlineexam/backend/service/QuestionServiceTest.java:96-116`

Before:

```java
getOwnedExam(examId, teacher.getId());
// getOwnedExam used findByIdAndCreatedByIdForUpdate
```

After:

```java
getOwnedExamForRead(examId, teacher.getId());
// getOwnedExamForRead uses findByIdAndCreatedById
```

Mutation paths continue to use `findByIdAndCreatedByIdForUpdate`. The regression test verifies the list path uses the non-locking query and does not call the write-locking query. Ownership and not-found behavior remain unchanged.

### 2. Exam close is serialized with student start and answer writes

Files and final locations:

- `backend/src/main/java/com/onlineexam/backend/repository/ExamRepository.java:34-38`
- `backend/src/main/java/com/onlineexam/backend/repository/ExamAttemptRepository.java:28-75`
- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java:74-92, 123-157`
- `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java:100-162, 240-250`

Before:

```java
Exam exam = exams.findById(examId);
// status was checked without serializing with Teacher close
```

After:

```java
Exam exam = exams.findByIdForUpdate(examId);
// then findByExamIdAndStudentIdForUpdate(...)
```

Start locks the exam before checking status/schedule and before finding or creating an attempt. Answer saving first performs a non-locking ownership lookup only to discover the parent exam ID, then locks the exam, then locks the ownership-scoped attempt and revalidates the relationship and status.

The ID-only exam lock is parameterized and uses `PESSIMISTIC_WRITE`. The unique `(exam_id, student_id)` database constraint remains the final duplicate-attempt safeguard, and duplicate insertion is still translated to HTTP 409 by the existing service code.

### 3. Close policy is explicit and reachable from the frontend

Files and final locations:

- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java:44-64, 74-92, 123-157`
- `frontend/src/pages/StudentExamsPage.tsx:27-38`
- `frontend/src/pages/ExamInstructionsPage.tsx:20-29`
- `frontend/src/pages/TeacherExamsPage.tsx:90-93`
- `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java:240-250, 318-340`

The enforced policy is:

- Closed exams reject new attempts.
- Closed exams reject new or changed answers.
- Existing attempts can still be retrieved read-only.
- Existing attempts can submit already-saved answers.
- Expired attempts still become `AUTO_SUBMITTED`.
- Closing does not erase answers, extend deadlines, reopen attempts, or create another result.

Closed exams with an existing attempt remain visible to that student. The instructions page navigates directly to the existing attempt instead of trying to start it again.

### 4. Result-marking integrity

Files and final locations:

- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java:211-238`
- `backend/src/main/resources/db/migration/V2__create_examination_tables.sql:307-317` (inspected only)
- `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java:164-238, 252-316`

Evaluation behavior is:

- Correct answer: add question marks.
- Incorrect answer: subtract negative marks.
- Unanswered answer: add zero.
- Obtained marks are clamped to `[0, totalMarks]`.
- The persisted exam total is compared with the sum of question marks before a result is created. A mismatch fails rather than being silently hidden.
- Because obtained marks are bounded by total marks, percentage remains within 0-100.

V2 requires `obtained_marks >= 0 AND obtained_marks <= total_marks`, `total_marks > 0`, and percentage between 0 and 100. No migration was changed.

Tests cover correct evaluation, all-incorrect negative-marking floor, and a mixed correct/incorrect/unanswered calculation.

## Locking design

| Operation | Lock order |
|---|---|
| Teacher publish/close | exam |
| Teacher question create/update/delete | exam, then question/options |
| Teacher question list | no write lock; non-locking exam ownership lookup |
| Student start/resume | exam, then student attempt |
| Student answer save | scalar attempt-to-exam discovery, exam, expiration attempt lock in `REQUIRES_NEW`, fresh ownership-scoped attempt lock, then question/answer |
| Student retrieval | scalar exam discovery, expiration attempt lock in `REQUIRES_NEW`, fresh ownership-scoped attempt lock, post-lock deadline check; exam read is non-locking |
| Manual submission | scalar exam discovery, expiration attempt lock in `REQUIRES_NEW`, fresh ownership-scoped attempt lock, then evaluation |
| Scheduled auto-submission | expired attempts, then evaluation reads |

The paths that need both parent and child mutation locks acquire the parent exam first. Submission and scheduled evaluation do not lock the exam because they do not mutate exam status, avoiding an attempt-to-exam lock cycle.

## Security review

- Role restrictions remain enforced by `/api/admin/**`, `/api/teacher/**`, and `/api/student/**` security rules.
- Service methods derive identity from the authenticated session name and verify enabled database-backed accounts.
- `AccountStatusFilter` invalidates sessions for disabled or missing accounts.
- CSRF remains enabled; frontend mutating requests obtain and send `X-XSRF-TOKEN`.
- Student option DTOs contain option ID and text only; correctness fields are not exposed.
- Student attempt and result lookups are ownership-scoped.
- Teacher exam/question operations use authenticated teacher ownership.
- Published questions remain immutable through draft-status and attempt checks.
- Submitted and auto-submitted attempts cannot accept answer changes.
- Result uniqueness remains enforced by the V2 unique constraint on `attempt_id` plus the service duplicate check.

## Database and integration-test status

V1 and V2 were inspected and are unchanged. No application records were modified.

`BackendApplicationTests.contextLoads` remains disabled because it requires a configured PostgreSQL test database. Docker is installed, but the Docker daemon was not reachable. PostgreSQL is running locally, but the configured `exam_app` password was unavailable; connecting to `online_exam_db` without a password failed. No attempt was made to use, seed, reset, truncate, or drop the application database.

PostgreSQL integration tests did **not** run. The read-only regression and concurrency-policy tests are service-level Mockito tests; they verify call sequencing and state checks but cannot prove PostgreSQL lock behavior.

## Tests run

- `backend\mvnw.cmd clean test` (run from `backend`) - passed: 33 tests passed, 0 failures, 0 errors, 1 skipped (`BackendApplicationTests.contextLoads`), 34 tests run including the skipped test. Suite counts: `ExamServiceTest` 8 passed, `QuestionServiceTest` 9 passed, `StudentExamServiceTest` 16 passed.
- `frontend\npm run lint` - passed.
- `frontend\npm run build` - passed.
- `git diff --check` - passed.

Mockito emitted Java 25 dynamic-agent warnings; they did not fail tests.

## Production hardening limitations

No production profile or deployment configuration exists in the repository. Local configuration remains HTTP/localhost by design:

- `backend/src/main/resources/application.yaml` uses the local database defaults and does not set a production `Secure` cookie profile.
- `frontend/src/services/authService.ts` uses the localhost API URL.
- CORS currently allows the local frontend origin.

Production deployment should add an environment-specific HTTPS profile with `server.servlet.session.cookie.secure=true`, an environment-provided API base URL, and an environment-provided allowed CORS origin. These were not hardcoded or enabled locally.

No login/registration rate limiter or gateway configuration was found. Production must provide rate limiting at the reverse proxy/API gateway or adopt a shared, distributed limiter before internet exposure. No fragile in-memory limiter was added.

## Final worktree

Final branch: `feature/teacher-exam-management-ui`

The worktree remains uncommitted. Existing changes and the new report are retained.

Complete changed-path list:

- `backend/src/main/java/com/onlineexam/backend/BackendApplication.java`
- `backend/src/main/java/com/onlineexam/backend/controller/StudentExamController.java`
- `backend/src/main/java/com/onlineexam/backend/controller/TeacherExamController.java`
- `backend/src/main/java/com/onlineexam/backend/dto/SaveAnswerRequest.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentAttemptResponse.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentExamResponse.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentOptionResponse.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentQuestionResponse.java`
- `backend/src/main/java/com/onlineexam/backend/dto/StudentResultResponse.java`
- `backend/src/main/java/com/onlineexam/backend/repository/ExamAttemptRepository.java`
- `backend/src/main/java/com/onlineexam/backend/repository/ExamRepository.java`
- `backend/src/main/java/com/onlineexam/backend/service/ExamService.java`
- `backend/src/main/java/com/onlineexam/backend/service/QuestionService.java`
- `backend/src/main/java/com/onlineexam/backend/service/StudentExamService.java`
- `backend/src/main/java/com/onlineexam/backend/service/ExamAttemptEvaluationService.java`
- `backend/src/main/java/com/onlineexam/backend/service/ExpiredAttemptService.java`
- `backend/src/test/java/com/onlineexam/backend/service/ExamServiceTest.java`
- `backend/src/test/java/com/onlineexam/backend/service/QuestionServiceTest.java`
- `backend/src/test/java/com/onlineexam/backend/service/StudentExamServiceTest.java`
- `frontend/src/App.tsx`
- `frontend/src/index.css`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/ExamInstructionsPage.tsx`
- `frontend/src/pages/ExamResultPage.tsx`
- `frontend/src/pages/StudentExamsPage.tsx`
- `frontend/src/pages/TakeExamPage.tsx`
- `frontend/src/pages/TeacherExamsPage.tsx`
- `frontend/src/pages/TeacherQuestionsPage.tsx`
- `frontend/src/services/authService.ts`
- `docs/SECURITY_CORRECTION_REPORT.md`
