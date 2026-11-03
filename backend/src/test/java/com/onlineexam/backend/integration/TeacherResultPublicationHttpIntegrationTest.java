package com.onlineexam.backend.integration;

import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TeacherResultPublicationHttpIntegrationTest {
    private static final String PASSWORD = "HttpIntegrationPassword-123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserAccountRepository users;
    @Autowired private ExamRepository exams;
    @Autowired private QuestionRepository questions;
    @Autowired private QuestionOptionRepository options;
    @Autowired private ExamAttemptRepository attempts;
    @Autowired private ExamResultRepository results;
    @Autowired private Clock applicationClock;

    private final String runId = UUID.randomUUID().toString().replace("-", "");
    private final List<Long> userIds = new CopyOnWriteArrayList<>();
    private final List<Long> examIds = new CopyOnWriteArrayList<>();

    @BeforeEach
    void verifyIsolatedDatabase() {
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
    void unauthenticatedUserCannotPublishResults() throws Exception {
        Fixture fixture = fixture("unauthenticated");

        mockMvc.perform(post("/api/teacher/exams/{examId}/results/publish", fixture.exam.getId())
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Authentication required")));
    }

    @Test
    void studentCannotAccessTeacherPublicationEndpoint() throws Exception {
        Fixture fixture = fixture("student-role");

        mockMvc.perform(post("/api/teacher/exams/{examId}/results/publish", fixture.exam.getId())
                        .session(login(fixture.student.getEmail()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotAccessStudentEndpoint() throws Exception {
        Fixture fixture = fixture("teacher-role");

        mockMvc.perform(get("/api/student/exams")
                        .session(login(fixture.teacher.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void owningTeacherPublishesClosedExamAndStudentCanReadPublishedResult() throws Exception {
        Fixture fixture = fixture("owner-publish");

        mockMvc.perform(post("/api/teacher/exams/{examId}/results/publish", fixture.exam.getId())
                        .session(login(fixture.teacher.getEmail()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultsPublishedAt").isNotEmpty());

        mockMvc.perform(get("/api/student/attempts/{attemptId}/result", fixture.attempt.getId())
                        .session(login(fixture.student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(fixture.attempt.getId()))
                .andExpect(jsonPath("$.totalMarks").value(1.0))
                .andExpect(jsonPath("$.obtainedMarks").value(1.0))
                .andExpect(jsonPath("$.percentage").value(100.0));
    }

    @Test
    void differentTeacherCannotPublishAnotherTeachersExam() throws Exception {
        Fixture fixture = fixture("ownership");

        mockMvc.perform(post("/api/teacher/exams/{examId}/results/publish", fixture.exam.getId())
                        .session(login(fixture.otherTeacher.getEmail()))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/student/attempts/{attemptId}/result", fixture.attempt.getId())
                        .session(login(fixture.student.getEmail())))
                .andExpect(status().isConflict());
    }

    @Test
    void studentCannotRetrieveAnotherStudentsAttemptOrResult() throws Exception {
        Fixture fixture = fixture("student-isolation");

        mockMvc.perform(get("/api/student/attempts/{attemptId}", fixture.attempt.getId())
                        .session(login(fixture.otherStudent.getEmail())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/student/attempts/{attemptId}/result", fixture.attempt.getId())
                        .session(login(fixture.otherStudent.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void unpublishedResultAndSubmissionDoNotExposeScores() throws Exception {
        Fixture fixture = fixture("unpublished");
        ExamAttempt inProgress = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.otherStudent.getId(), Instant.now().plusSeconds(120)));

        mockMvc.perform(get("/api/student/attempts/{attemptId}/result", fixture.attempt.getId())
                        .session(login(fixture.student.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.obtainedMarks").doesNotExist())
                .andExpect(jsonPath("$.totalMarks").doesNotExist())
                .andExpect(jsonPath("$.percentage").doesNotExist());

        mockMvc.perform(post("/api/student/attempts/{attemptId}/submit", inProgress.getId())
                        .session(login(fixture.otherStudent.getEmail()))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.obtainedMarks").doesNotExist())
                .andExpect(jsonPath("$.totalMarks").doesNotExist())
                .andExpect(jsonPath("$.percentage").doesNotExist());
    }

    @Test
    void attemptAndListingDoNotExposeAnswerKeysOrStudentResults() throws Exception {
        Fixture fixture = fixture("student-surfaces");

        mockMvc.perform(get("/api/student/attempts/{attemptId}", fixture.attempt.getId())
                        .session(login(fixture.student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[0].answerKey").doesNotExist())
                .andExpect(jsonPath("$.obtainedMarks").doesNotExist())
                .andExpect(jsonPath("$.percentage").doesNotExist());

        mockMvc.perform(get("/api/student/exams")
                        .session(login(fixture.student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("obtainedMarks"))))
                .andExpect(content().string(not(containsString("percentage"))));
    }

    @Test
    void automaticSubmissionDoesNotExposeResultBeforePublication() throws Exception {
        Fixture fixture = fixture("automatic-submission");
        ExamAttempt expiring = attempts.saveAndFlush(new ExamAttempt(
                fixture.exam.getId(), fixture.otherStudent.getId(), Instant.now().plusSeconds(120)));
        jdbc.update("""
                UPDATE exam_attempts
                SET started_at = now() - interval '2 seconds',
                    expires_at = now() - interval '1 second'
                WHERE id = ?
                """, expiring.getId());

        ExamAttempt persisted = attempts.findById(expiring.getId()).orElseThrow();
        assertTrue(persisted.getExpiresAt().isAfter(persisted.getStartedAt()));
        assertTrue(persisted.getExpiresAt().isBefore(applicationClock.instant()));
        assertEquals(AttemptStatus.IN_PROGRESS, persisted.getStatus());
        assertNull(persisted.getSubmittedAt());

        mockMvc.perform(get("/api/student/attempts/{attemptId}", expiring.getId())
                        .session(login(fixture.otherStudent.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTO_SUBMITTED"))
                .andExpect(jsonPath("$.obtainedMarks").doesNotExist())
                .andExpect(jsonPath("$.percentage").doesNotExist());

        mockMvc.perform(get("/api/student/attempts/{attemptId}/result", expiring.getId())
                        .session(login(fixture.otherStudent.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.obtainedMarks").doesNotExist());
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .param("email", email)
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
    }

    private Fixture fixture(String label) {
        UserAccount teacher = saveUser(label + "-teacher", UserRole.TEACHER);
        UserAccount otherTeacher = saveUser(label + "-other-teacher", UserRole.TEACHER);
        UserAccount student = saveUser(label + "-student", UserRole.STUDENT);
        UserAccount otherStudent = saveUser(label + "-other-student", UserRole.STUDENT);
        Instant start = Instant.now().minusSeconds(60);
        Exam exam = exams.saveAndFlush(new Exam(
                runId + "-" + label, "description", "subject", "instructions",
                30, start, start.plusSeconds(3600), teacher.getId()));
        examIds.add(exam.getId());
        exam.setStatus(ExamStatus.CLOSED);
        exam.setTotalMarks(BigDecimal.ONE);
        exams.saveAndFlush(exam);

        Question question = questions.saveAndFlush(new Question(
                exam.getId(), "Question", QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE, BigDecimal.ZERO, 1));
        QuestionOption correct = options.saveAndFlush(new QuestionOption(
                question.getId(), "Correct", 1, true));
        options.saveAndFlush(new QuestionOption(
                question.getId(), "Incorrect", 2, false));

        ExamAttempt attempt = attempts.saveAndFlush(new ExamAttempt(
                exam.getId(), student.getId(), Instant.now().plusSeconds(120)));
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempts.saveAndFlush(attempt);
        results.saveAndFlush(new ExamResult(
                attempt.getId(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.valueOf(100)));
        return new Fixture(exam, teacher, otherTeacher, student, otherStudent, attempt, correct);
    }

    private UserAccount saveUser(String label, UserRole role) {
        UserAccount user = users.saveAndFlush(new UserAccount(
                label, runId + "-" + label + "@example.test",
                passwordEncoder.encode(PASSWORD), role));
        userIds.add(user.getId());
        return user;
    }

    private record Fixture(
            Exam exam,
            UserAccount teacher,
            UserAccount otherTeacher,
            UserAccount student,
            UserAccount otherStudent,
            ExamAttempt attempt,
            QuestionOption correctOption
    ) {}
}
