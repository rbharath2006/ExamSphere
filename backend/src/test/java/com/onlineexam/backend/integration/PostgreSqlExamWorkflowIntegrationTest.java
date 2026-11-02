package com.onlineexam.backend.integration;

import com.onlineexam.backend.dto.CreateQuestionOptionRequest;
import com.onlineexam.backend.dto.CreateQuestionRequest;
import com.onlineexam.backend.dto.SaveAnswerRequest;
import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;
import com.onlineexam.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSqlExamWorkflowIntegrationTest.TestClockConfiguration.class)
class PostgreSqlExamWorkflowIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private TransactionTemplate transactions;
    @Autowired private UserAccountRepository users;
    @Autowired private ExamRepository exams;
    @Autowired private QuestionRepository questions;
    @Autowired private QuestionOptionRepository options;
    @Autowired private ExamAttemptRepository attempts;
    @Autowired private StudentAnswerRepository answers;
    @Autowired private ExamResultRepository results;
    @Autowired private ExpiredAttemptService expiration;
    @Autowired private StudentExamService studentExams;
    @Autowired private QuestionService questionService;
    @Autowired private TestClock testClock;

    private final String runId = UUID.randomUUID().toString().replace("-", "");
    private final List<Long> userIds = new CopyOnWriteArrayList<>();
    private final List<Long> examIds = new CopyOnWriteArrayList<>();
    private final List<Long> attemptIds = new CopyOnWriteArrayList<>();

    @BeforeEach
    void verifyIsolatedDatabase() {
        testClock.reset();
        assertEquals("online_exam_test_db",
                jdbc.queryForObject("select current_database()", String.class));
    }

    @AfterEach
    void cleanOnlyThisTestFixtures() {
        assertEquals("online_exam_test_db",
                jdbc.queryForObject("select current_database()", String.class));
        for (Long examId : examIds) {
            jdbc.update("delete from exam_results where attempt_id in " +
                    "(select id from exam_attempts where exam_id = ?)", examId);
            jdbc.update("delete from student_answers where attempt_id in " +
                    "(select id from exam_attempts where exam_id = ?)", examId);
            jdbc.update("delete from exam_attempts where exam_id = ?", examId);
            jdbc.update("delete from question_options where question_id in " +
                    "(select id from questions where exam_id = ?)", examId);
            jdbc.update("delete from questions where exam_id = ?", examId);
            jdbc.update("delete from exams where id = ?", examId);
        }
        for (Long userId : userIds) {
            jdbc.update("delete from users where id = ?", userId);
        }
    }

    @Test
    void expirationCommitsExactlyOnceAndSurvivesOuterRollback() {
        Fixture fixture = fixture("expiration");
        ExamAttempt attempt = expiredAttempt(fixture);

        Long attemptId = attempt.getId();
        transactions.executeWithoutResult(status -> {
            expiration.expireIfNeeded(attemptId, fixture.student.getId());
            status.setRollbackOnly();
        });

        ExamAttempt persisted = attempts.findById(attemptId).orElseThrow();
        assertEquals(AttemptStatus.AUTO_SUBMITTED, persisted.getStatus());
        assertNotNull(persisted.getSubmittedAt());
        assertTrue(results.existsByAttemptId(attemptId));
        Long resultId = results.findByAttemptId(attemptId).orElseThrow().getId();

        expiration.expireIfNeeded(attemptId, fixture.student.getId());

        assertEquals(resultId, results.findByAttemptId(attemptId).orElseThrow().getId());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from exam_results where attempt_id = ?",
                Integer.class, attemptId));
    }

    @Test
    void concurrentStartsCreateOneAttempt() throws Exception {
        Fixture fixture = fixture("concurrent-start");
        publish(fixture.exam, fixture.question);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<?> first = pool.submit(() -> awaitAndStart(gate, fixture));
            Future<?> second = pool.submit(() -> awaitAndStart(gate, fixture));
            gate.countDown();
            int conflicts = 0;
            for (Future<?> future : List.of(first, second)) {
                try {
                    future.get(20, TimeUnit.SECONDS);
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof ResponseStatusException response
                            && response.getStatusCode().value() == 409) {
                        conflicts++;
                    } else {
                        throw exception;
                    }
                }
            }
            assertTrue(conflicts <= 1);
            assertEquals(1, attempts.findByExamIdAndStudentId(
                    fixture.exam.getId(), fixture.student.getId()).stream().count());
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from exam_attempts where exam_id = ? and student_id = ?",
                    Integer.class, fixture.exam.getId(), fixture.student.getId()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentSubmissionAndExpirationCreateOneResult() throws Exception {
        Fixture fixture = fixture("concurrent-submit");
        publish(fixture.exam, fixture.question);
        ExamAttempt attempt = expiredAttempt(fixture);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<?> submit = pool.submit(() -> {
                await(gate);
                return studentExams.submit(fixture.student.getEmail(), attempt.getId());
            });
            Future<?> expire = pool.submit(() -> {
                await(gate);
                return expiration.expireIfNeeded(attempt.getId(), fixture.student.getId());
            });
            gate.countDown();
            submit.get(20, TimeUnit.SECONDS);
            expire.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from exam_results where attempt_id = ?",
                Integer.class, attempt.getId()));
        assertNotEquals(AttemptStatus.IN_PROGRESS,
                attempts.findById(attempt.getId()).orElseThrow().getStatus());
        assertEquals(AttemptStatus.AUTO_SUBMITTED,
                attempts.findById(attempt.getId()).orElseThrow().getStatus());
        assertNotNull(attempts.findById(attempt.getId()).orElseThrow().getSubmittedAt());
    }

    @Test
    void futureDeadlineManualSubmissionCreatesOneResult() {
        Fixture fixture = fixture("manual-submit");
        publish(fixture.exam, fixture.question);
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.student.getId(),
                Instant.now().plusSeconds(120)));
        attemptIds.add(attempt.getId());
        testClock.reset();

        studentExams.submit(fixture.student.getEmail(), attempt.getId());

        ExamAttempt persisted = attempts.findById(attempt.getId()).orElseThrow();
        assertEquals(AttemptStatus.SUBMITTED, persisted.getStatus());
        assertNotNull(persisted.getSubmittedAt());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from exam_results where attempt_id = ?",
                Integer.class, attempt.getId()));
    }

    @Test
    void requestWaitingForAttemptLockRejectsAnswerAfterDeadline() throws Exception {
        Fixture fixture = fixture("lock-deadline");
        publish(fixture.exam, fixture.question);
        Instant deadline = testClock.instant().plusMillis(700);
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.student.getId(), deadline));
        attemptIds.add(attempt.getId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch answerRequestStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = null;
        try {
            holder = pool.submit(() -> transactions.executeWithoutResult(status -> {
                attempts.findByIdAndStudentIdForUpdate(
                        attempt.getId(), fixture.student.getId()).orElseThrow();
                locked.countDown();
                await(release);
            }));
            await(locked);
            Future<Boolean> answer = pool.submit(() -> {
                answerRequestStarted.countDown();
                return studentExams.saveAnswer(
                        fixture.student.getEmail(), attempt.getId(),
                        new SaveAnswerRequest(fixture.question.getId(),
                                fixture.correctOption.getId()));
            });
            await(answerRequestStarted);
            testClock.set(deadline.plusMillis(1));
            release.countDown();

            assertFalse(answer.get(20, TimeUnit.SECONDS));
            holder.get(20, TimeUnit.SECONDS);
            assertEquals(AttemptStatus.AUTO_SUBMITTED,
                    attempts.findById(attempt.getId()).orElseThrow().getStatus());
            assertNotNull(attempts.findById(attempt.getId()).orElseThrow()
                    .getSubmittedAt());
            assertFalse(answers.existsByAttemptIdAndQuestionId(
                    attempt.getId(), fixture.question.getId()));
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from exam_results where attempt_id = ?",
                    Integer.class, attempt.getId()));
        } finally {
            release.countDown();
            if (holder != null) {
                try {
                    holder.get(20, TimeUnit.SECONDS);
                } catch (ExecutionException ignored) {
                    // Preserve the primary assertion failure while releasing the lock.
                }
            }
            pool.shutdownNow();
        }
    }

    @Test
    void closedAndSubmittedAttemptsRejectAnswersAndOwnershipIsEnforced() {
        Fixture fixture = fixture("ownership");
        publish(fixture.exam, fixture.question);
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.student.getId(),
                Instant.now().plusSeconds(120)));
        attemptIds.add(attempt.getId());

        fixture.exam.setStatus(ExamStatus.CLOSED);
        exams.saveAndFlush(fixture.exam);
        ResponseStatusException closed = assertThrows(ResponseStatusException.class, () ->
                studentExams.saveAnswer(fixture.student.getEmail(), attempt.getId(),
                        new SaveAnswerRequest(fixture.question.getId(),
                                fixture.correctOption.getId())));
        assertEquals(409, closed.getStatusCode().value());

        UserAccount other = saveUser("other-student", UserRole.STUDENT);
        assertThrows(ResponseStatusException.class, () ->
                studentExams.getAttempt(other.getEmail(), attempt.getId()));
        assertThrows(ResponseStatusException.class, () ->
                studentExams.result(other.getEmail(), attempt.getId()));

        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempts.saveAndFlush(attempt);
        ResponseStatusException autoSubmitted = assertThrows(ResponseStatusException.class, () ->
                studentExams.saveAnswer(fixture.student.getEmail(), attempt.getId(),
                        new SaveAnswerRequest(fixture.question.getId(),
                                fixture.correctOption.getId())));
        assertEquals(409, autoSubmitted.getStatusCode().value());

        ExamAttempt submitted = attempts.saveAndFlush(new ExamAttempt(
                        fixture.exam.getId(), other.getId(), Instant.now().plusSeconds(120)));
        attemptIds.add(submitted.getId());
        submitted.setStatus(AttemptStatus.SUBMITTED);
        submitted.setSubmittedAt(Instant.now());
        attempts.saveAndFlush(submitted);
        ResponseStatusException submittedFailure = assertThrows(ResponseStatusException.class, () ->
                        studentExams.saveAnswer(other.getEmail(), submitted.getId(),
                                new SaveAnswerRequest(fixture.question.getId(),
                                        fixture.correctOption.getId())));
        assertEquals(409, submittedFailure.getStatusCode().value());
    }

    @Test
    void postLockDeadlineCheckAutoSubmitsAfterPrecheckReturns() {
        Fixture fixture = fixture("post-lock-deadline");
        publish(fixture.exam, fixture.question);
        Instant futureDeadline = testClock.instant().plusSeconds(120);
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                        fixture.exam.getId(), fixture.student.getId(), futureDeadline));
        attemptIds.add(attempt.getId());

        testClock.set(futureDeadline.plusSeconds(1));
        assertFalse(studentExams.saveAnswer(fixture.student.getEmail(), attempt.getId(),
                new SaveAnswerRequest(fixture.question.getId(),
                        fixture.correctOption.getId())));

        ExamAttempt persisted = attempts.findById(attempt.getId()).orElseThrow();
        assertEquals(AttemptStatus.AUTO_SUBMITTED, persisted.getStatus());
        assertNotNull(persisted.getSubmittedAt());
        assertEquals(1, jdbc.queryForObject(
                        "select count(*) from exam_results where attempt_id = ?",
                        Integer.class, attempt.getId()));
        assertFalse(answers.existsByAttemptIdAndQuestionId(
                        attempt.getId(), fixture.question.getId()));
    }

    @Test
    void databaseConstraintsRejectDuplicateAndInvalidRows() {
        Fixture fixture = fixture("constraints");
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.student.getId(),
                Instant.now().plusSeconds(120)));
        attemptIds.add(attempt.getId());

        ExamAttempt duplicate = new ExamAttempt(fixture.exam.getId(),
                fixture.student.getId(), Instant.now().plusSeconds(120));
        assertThrows(DataIntegrityViolationException.class,
                () -> attempts.saveAndFlush(duplicate));

        jdbc.update("insert into exam_results(attempt_id,total_marks,obtained_marks,percentage) " +
                        "values (?, ?, ?, ?)", attempt.getId(), 1, 1, 100);
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into exam_results(attempt_id,total_marks,obtained_marks,percentage) " +
                        "values (?, ?, ?, ?)", attempt.getId(), 1, 1, 100));
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into exam_attempts(exam_id,student_id,expires_at) " +
                        "values (?, ?, now() + interval '1 hour')", -1, fixture.student.getId()));
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into student_answers(attempt_id,question_id,selected_option_id) " +
                        "values (?, ?, ?)", attempt.getId(), fixture.question.getId(),
                        fixture.otherQuestionOption.getId()));
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into exam_results(attempt_id,total_marks,obtained_marks,percentage) " +
                        "values (?, ?, ?, ?)", attempt.getId(), 1, 2, 200));
    }

    @Test
    void teacherOwnershipAndPublishedQuestionImmutabilityAreEnforced() {
        Fixture fixture = fixture("teacher-isolation");
        UserAccount otherTeacher = saveUser("other-teacher", UserRole.TEACHER);
        CreateQuestionRequest request = questionRequest();
        assertThrows(ResponseStatusException.class, () ->
                questionService.updateQuestion(otherTeacher.getEmail(),
                        fixture.exam.getId(), fixture.question.getId(), request));
        publish(fixture.exam, fixture.question);
        assertThrows(ResponseStatusException.class, () ->
                questionService.updateQuestion(fixture.teacher.getEmail(),
                        fixture.exam.getId(), fixture.question.getId(), request));
    }

    private void awaitAndStart(CountDownLatch gate, Fixture fixture) {
        await(gate);
        studentExams.start(fixture.student.getEmail(), fixture.exam.getId());
    }

    private static void await(CountDownLatch gate) {
        try {
            assertTrue(gate.await(20, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private Fixture fixture(String label) {
        UserAccount teacher = saveUser(label + "-teacher", UserRole.TEACHER);
        UserAccount student = saveUser(label + "-student", UserRole.STUDENT);
        Instant start = Instant.now().minusSeconds(10);
        Exam exam = exams.saveAndFlush(new Exam(runId + "-" + label,
                "integration", "integration", "integration", 30,
                start, start.plusSeconds(3600), teacher.getId()));
        examIds.add(exam.getId());
        Question question = questions.saveAndFlush(new Question(exam.getId(),
                "Question " + label, QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE, BigDecimal.ZERO, 1));
        Question otherQuestion = questions.saveAndFlush(new Question(exam.getId(),
                "Other question " + label, QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE, BigDecimal.ZERO, 2));
        QuestionOption correct = options.saveAndFlush(new QuestionOption(
                question.getId(), "Correct", 1, true));
        options.saveAndFlush(new QuestionOption(question.getId(), "Wrong 1", 2, false));
        options.saveAndFlush(new QuestionOption(question.getId(), "Wrong 2", 3, false));
        options.saveAndFlush(new QuestionOption(question.getId(), "Wrong 3", 4, false));
        QuestionOption otherOption = options.saveAndFlush(new QuestionOption(
                otherQuestion.getId(), "Other", 1, true));
        options.saveAndFlush(new QuestionOption(otherQuestion.getId(), "Other 2", 2, false));
        options.saveAndFlush(new QuestionOption(otherQuestion.getId(), "Other 3", 3, false));
        options.saveAndFlush(new QuestionOption(otherQuestion.getId(), "Other 4", 4, false));
        exam.setTotalMarks(BigDecimal.valueOf(2));
        exam.setStatus(ExamStatus.DRAFT);
        exams.saveAndFlush(exam);
        return new Fixture(teacher, student, exam, question, otherQuestion,
                correct, otherOption);
    }

    private ExamAttempt expiredAttempt(Fixture fixture) {
        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.student.getId(),
                testClock.instant().plusSeconds(120)));
        attemptIds.add(attempt.getId());
        jdbc.update("update exam_attempts set started_at = now() - interval '2 seconds', " +
                        "expires_at = now() - interval '1 second' where id = ?",
                attempt.getId());
        return attempt;
    }

    private UserAccount saveUser(String label, UserRole role) {
        UserAccount user = users.saveAndFlush(new UserAccount(
                label, runId + "-" + label + "@example.test", runId, role));
        userIds.add(user.getId());
        return user;
    }

    private void publish(Exam exam, Question question) {
        exam.setStatus(ExamStatus.PUBLISHED);
        exams.saveAndFlush(exam);
    }

    private CreateQuestionRequest questionRequest() {
        return new CreateQuestionRequest("Changed", QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE, BigDecimal.ZERO, List.of(
                new CreateQuestionOptionRequest("A", true),
                new CreateQuestionOptionRequest("B", false),
                new CreateQuestionOptionRequest("C", false),
                new CreateQuestionOptionRequest("D", false)));
    }

    private record Fixture(UserAccount teacher, UserAccount student, Exam exam,
                           Question question, Question otherQuestion,
                           QuestionOption correctOption,
                           QuestionOption otherQuestionOption) {
    }

    @TestConfiguration
    static class TestClockConfiguration {
        @Bean
        TestClock testClock() {
            return new TestClock();
        }

        @Bean
        @Primary
        Clock clock(TestClock clock) {
            return clock;
        }
    }

    static class TestClock extends Clock {
        private volatile Instant current = Instant.now();

        void set(Instant instant) {
            current = instant;
        }

        void reset() {
            current = Instant.now();
        }

        @Override
        public Instant instant() {
            return current;
        }

        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
