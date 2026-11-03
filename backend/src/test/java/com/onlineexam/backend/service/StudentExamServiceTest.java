package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.SaveAnswerRequest;
import com.onlineexam.backend.dto.StudentAttemptResponse;
import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentExamServiceTest {
    @Mock UserAccountRepository users;
    @Mock ExamRepository exams;
    @Mock ExamAttemptRepository attempts;
    @Mock QuestionRepository questions;
    @Mock QuestionOptionRepository options;
    @Mock StudentAnswerRepository answers;
    @Mock ExamResultRepository results;
    @Mock ExamAttemptEvaluationService evaluationMock;
    @Mock ExpiredAttemptService expirationMock;
    @Mock UserAccount student;
    @Mock Exam exam;
    @Mock ExamAttempt attempt;

    private StudentExamService service;

    @BeforeEach
    void setUp() {
        service = new StudentExamService(
                users, exams, attempts, questions, options, answers, results);
        when(users.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(7L);
        when(student.getRole()).thenReturn(UserRole.STUDENT);
        when(student.isEnabled()).thenReturn(true);
        when(exam.getResultsPublishedAt()).thenReturn(Instant.now());
        when(attempts.findByIdAndStudentIdForUpdate(anyLong(), anyLong()))
                .thenReturn(Optional.of(attempt));
        when(attempts.findByIdAndStudentId(anyLong(), anyLong()))
                .thenReturn(Optional.of(attempt));
        when(attempts.findExamIdByIdAndStudentId(anyLong(), anyLong()))
                .thenReturn(Optional.of(1L));
        when(attempts.findIdByExamIdAndStudentId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
    }

    @Test
    void studentSeesOnlyPublishedExamsAndAttemptState() {
        when(exams.findByStatusInOrderByStartsAtAsc(
                List.of(ExamStatus.PUBLISHED, ExamStatus.CLOSED)))
                .thenReturn(List.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(exam.getTitle()).thenReturn("Math");
        when(exam.getSubject()).thenReturn("Algebra");
        when(exam.getStartsAt()).thenReturn(Instant.now().plusSeconds(100));
        when(exam.getEndsAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(exam.getTotalMarks()).thenReturn(new BigDecimal("10.00"));
        when(exam.getDurationMinutes()).thenReturn(30);
        when(attempts.findByExamIdAndStudentId(1L, 7L))
                .thenReturn(Optional.empty());

        var response = service.listExams("student@example.com");

        assertEquals(1, response.size());
        assertTrue(response.getFirst().upcoming());
        verify(exams).findByStatusInOrderByStartsAtAsc(
                List.of(ExamStatus.PUBLISHED, ExamStatus.CLOSED));
    }

    @Test
    void nonStudentIsRejected() {
        when(student.getRole()).thenReturn(UserRole.TEACHER);
        assertThrows(ResponseStatusException.class,
                () -> service.listExams("student@example.com"));
    }

    @Test
    void duplicateInProgressAttemptIsResumed() {
        setupPublishedExam();
        when(attempts.findByExamIdAndStudentId(1L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempts.findIdByExamIdAndStudentId(1L, 7L))
                .thenReturn(Optional.of(22L));
        when(attempts.findByExamIdAndStudentIdForUpdate(1L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStartedAt()).thenReturn(Instant.now());
        when(questions.findByExamIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of());

        var response = service.start("student@example.com", 1L);

        assertEquals(22L, response.attemptId());
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void answerForWrongQuestionOptionIsRejected() {
        when(attempts.findByIdAndStudentId(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(attempt.getExamId()).thenReturn(1L);
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(questions.findByIdAndExamId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                service.saveAnswer("student@example.com", 22L,
                        new SaveAnswerRequest(5L, 9L)));
    }

    @Test
    void expiredAttemptCannotAcceptAnswers() {
        when(attempts.findByIdAndStudentId(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(results.existsByAttemptId(22L)).thenReturn(true);
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(attempt).setStatus(any());

        assertThrows(ResponseStatusException.class, () ->
                service.saveAnswer("student@example.com", 22L,
                        new SaveAnswerRequest(5L, 9L)));
        verify(questions, never()).findByIdAndExamId(any(), any());
    }

    @Test
    void closedExamCannotAcceptAnswers() {
        when(attempts.findByIdAndStudentId(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(attempt.getExamId()).thenReturn(1L);
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.saveAnswer("student@example.com", 22L,
                        new SaveAnswerRequest(5L, 9L)));

        assertEquals(409, exception.getStatusCode().value());
        verify(questions, never()).findByIdAndExamId(any(), any());
    }

    @Test
    void negativeScoreIsClampedToZeroBeforePersistence() {
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(attempt).setStatus(any());
        when(exams.findById(1L)).thenReturn(Optional.of(exam));
        when(exam.getTotalMarks()).thenReturn(new BigDecimal("2.00"));
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(5L);
        when(question.getMarks()).thenReturn(new BigDecimal("2.00"));
        when(question.getNegativeMarks()).thenReturn(new BigDecimal("1.00"));
        when(questions.findByExamIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(question));
        when(answers.findByAttemptIdAndQuestionId(22L, 5L))
                .thenReturn(Optional.of(new StudentAnswer(22L, 5L, 9L)));
        when(options.findByIdAndQuestionId(9L, 5L))
                .thenReturn(Optional.of(new QuestionOption(5L, "A", 1, false)));
        when(results.existsByAttemptId(22L)).thenReturn(false);
        when(results.findByAttemptId(22L)).thenReturn(Optional.of(mock(ExamResult.class)));

        AtomicReference<ExamResult> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return persisted.get();
        }).when(results).saveAndFlush(any(ExamResult.class));

        service.submit("student@example.com", 22L);

        assertEquals(BigDecimal.ZERO, persisted.get().getObtainedMarks());
    }

    @Test
    void submissionEvaluatesCorrectAnswerOnBackend() {
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(attempt).setStatus(any());
        when(exams.findById(1L)).thenReturn(Optional.of(exam));
        when(exam.getTotalMarks()).thenReturn(new BigDecimal("2.00"));
        Question question = org.mockito.Mockito.mock(Question.class);
        when(question.getId()).thenReturn(5L);
        when(question.getMarks()).thenReturn(new BigDecimal("2.00"));
        when(question.getNegativeMarks()).thenReturn(BigDecimal.ZERO);
        when(questions.findByExamIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(question));
        StudentAnswer answer = new StudentAnswer(22L, 5L, 9L);
        when(answers.findByAttemptIdAndQuestionId(22L, 5L))
                .thenReturn(Optional.of(answer));
        QuestionOption correct = new QuestionOption(5L, "A", 1, true);
        when(options.findByIdAndQuestionId(9L, 5L))
                .thenReturn(Optional.of(correct));
        when(results.existsByAttemptId(22L)).thenReturn(false);
        ExamResult result = mock(ExamResult.class);
        when(results.findByAttemptId(22L)).thenReturn(Optional.of(result));
        when(result.getTotalMarks()).thenReturn(new BigDecimal("2.00"));
        when(result.getObtainedMarks()).thenReturn(new BigDecimal("2.00"));
        when(result.getPercentage()).thenReturn(new BigDecimal("100.00"));

        service.submit("student@example.com", 22L);

        verify(results).saveAndFlush(any(ExamResult.class));
        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
    }

    @Test
    void closedExamRejectsNewAttempt() {
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);
        when(attempts.findIdByExamIdAndStudentId(1L, 7L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.start("student@example.com", 1L));

        assertEquals(409, exception.getStatusCode().value());
        verify(attempts, never()).findByExamIdAndStudentIdForUpdate(any(), any());
    }

    @Test
    void mixedEvaluationIncludesCorrectIncorrectAndUnansweredQuestions() {
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L)).thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(attempt).setStatus(any());
        when(exams.findById(1L)).thenReturn(Optional.of(exam));
        when(exam.getTotalMarks()).thenReturn(new BigDecimal("5.00"));

        Question correctOne = mock(Question.class);
        Question incorrect = mock(Question.class);
        Question unanswered = mock(Question.class);
        Question correctTwo = mock(Question.class);
        List<Question> questionList = List.of(correctOne, incorrect, unanswered, correctTwo);
        BigDecimal[] marks = {
                new BigDecimal("2.00"), new BigDecimal("1.00"),
                new BigDecimal("1.00"), new BigDecimal("1.00")
        };
        BigDecimal[] negatives = {
                BigDecimal.ZERO, new BigDecimal("0.50"),
                BigDecimal.ZERO, BigDecimal.ZERO
        };
        for (int i = 0; i < questionList.size(); i++) {
            when(questionList.get(i).getId()).thenReturn((long) (i + 1));
            when(questionList.get(i).getMarks()).thenReturn(marks[i]);
            when(questionList.get(i).getNegativeMarks()).thenReturn(negatives[i]);
        }
        when(questions.findByExamIdOrderByDisplayOrderAsc(1L)).thenReturn(questionList);
        when(answers.findByAttemptIdAndQuestionId(22L, 1L))
                .thenReturn(Optional.of(new StudentAnswer(22L, 1L, 11L)));
        when(answers.findByAttemptIdAndQuestionId(22L, 2L))
                .thenReturn(Optional.of(new StudentAnswer(22L, 2L, 12L)));
        when(answers.findByAttemptIdAndQuestionId(22L, 3L))
                .thenReturn(Optional.empty());
        when(answers.findByAttemptIdAndQuestionId(22L, 4L))
                .thenReturn(Optional.of(new StudentAnswer(22L, 4L, 14L)));
        when(options.findByIdAndQuestionId(11L, 1L))
                .thenReturn(Optional.of(new QuestionOption(1L, "A", 1, true)));
        when(options.findByIdAndQuestionId(12L, 2L))
                .thenReturn(Optional.of(new QuestionOption(2L, "B", 1, false)));
        when(options.findByIdAndQuestionId(14L, 4L))
                .thenReturn(Optional.of(new QuestionOption(4L, "D", 1, true)));
        when(results.existsByAttemptId(22L)).thenReturn(false);
        ExamResult savedResult = mock(ExamResult.class);
        when(results.findByAttemptId(22L)).thenReturn(Optional.of(savedResult));
        when(savedResult.getTotalMarks()).thenReturn(new BigDecimal("5.00"));
        when(savedResult.getObtainedMarks()).thenReturn(new BigDecimal("2.50"));
        when(savedResult.getPercentage()).thenReturn(new BigDecimal("50.00"));

        AtomicReference<ExamResult> persisted = new AtomicReference<>();
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return persisted.get();
        }).when(results).saveAndFlush(any(ExamResult.class));

        service.submit("student@example.com", 22L);

        assertEquals(new BigDecimal("2.50"), persisted.get().getObtainedMarks());
        assertEquals(new BigDecimal("5.00"), persisted.get().getTotalMarks());
    }

    @Test
    void closedExamAllowsExistingAttemptToBeReadAndSubmitted() {
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(attempt.getStartedAt()).thenReturn(Instant.now());
        when(exams.findById(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getTitle()).thenReturn("Closed exam");
        when(exam.getDurationMinutes()).thenReturn(30);
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);
        when(questions.findByExamIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of());
        when(answers.findByAttemptId(22L)).thenReturn(List.of());

        StudentAttemptResponse response =
                service.getAttempt("student@example.com", 22L);

        assertEquals(AttemptStatus.IN_PROGRESS, response.status());
        assertFalse(response.answersAllowed());
        verify(answers).findByAttemptId(22L);

        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(AttemptStatus.SUBMITTED);
            return null;
        }).when(attempt).setStatus(any());
        when(results.existsByAttemptId(22L)).thenReturn(true);
        ExamResult existingResult = mock(ExamResult.class);
        when(results.findByAttemptId(22L)).thenReturn(Optional.of(existingResult));
        when(existingResult.getTotalMarks()).thenReturn(BigDecimal.ONE);
        when(existingResult.getObtainedMarks()).thenReturn(BigDecimal.ONE);
        when(existingResult.getPercentage()).thenReturn(new BigDecimal("100.00"));

        var result = service.submit("student@example.com", 22L);

        assertEquals(AttemptStatus.SUBMITTED, result.status());
        verify(results, never()).saveAndFlush(any(ExamResult.class));
    }

    @Test
    void expiredAttemptIsCommittedBeforeAnswerConflictAndEvaluatedOnlyOnce()
            throws Exception {
        assertEquals(Propagation.REQUIRES_NEW,
                ExpiredAttemptService.class
                        .getDeclaredMethod("expireIfNeeded", Long.class, Long.class)
                        .getAnnotation(Transactional.class).propagation());
        ExamAttemptEvaluationService evaluation =
                new ExamAttemptEvaluationService(exams, questions, options, answers, results);
        ExpiredAttemptService expiration = new ExpiredAttemptService(attempts, evaluation);
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(results.existsByAttemptId(22L)).thenReturn(true);
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(attempt).setStatus(any());

        assertEquals(AttemptStatus.AUTO_SUBMITTED,
                expiration.expireIfNeeded(22L, 7L));
        assertNull(expiration.expireIfNeeded(22L, 7L));

        verify(attempts, times(1)).saveAndFlush(attempt);
        verify(results, times(1)).existsByAttemptId(22L);
    }

    @Test
    void submittedAttemptReturnedByFreshLockCannotBeModified() {
        when(attempts.findExamIdByIdAndStudentId(22L, 7L))
                .thenReturn(Optional.of(1L));
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.SUBMITTED);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(100));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.saveAnswer("student@example.com", 22L,
                        new SaveAnswerRequest(5L, 9L)));

        assertEquals(409, exception.getStatusCode().value());
        verify(answers, never()).saveAndFlush(any());
        verify(attempts, times(2)).findByIdAndStudentIdForUpdate(22L, 7L);
    }

    @Test
    void expirationDoesNotRewriteAlreadySubmittedAttempt() {
        when(attempt.getStatus()).thenReturn(AttemptStatus.SUBMITTED);
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        ExamAttemptEvaluationService evaluation =
                new ExamAttemptEvaluationService(exams, questions, options, answers, results);
        ExpiredAttemptService expiration = new ExpiredAttemptService(attempts, evaluation);

        assertNull(expiration.expireIfNeeded(22L, 7L));

        verify(attempt, never()).setStatus(any());
        verify(attempts, never()).saveAndFlush(any());
        verify(results, never()).saveAndFlush(any());
    }

    @Test
    void lateAnswerAfterPrecheckIsAutoSubmittedAndRejectedAfterCommit() {
        StudentExamService concurrentService = serviceWithMockedExpiration();
        when(attempts.findExamIdByIdAndStudentId(22L, 7L))
                .thenReturn(Optional.of(1L));
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(results.existsByAttemptId(22L)).thenReturn(true);

        assertFalse(concurrentService.saveAnswer(
                "student@example.com", 22L, new SaveAnswerRequest(5L, 9L)));

        verify(attempts).saveAndFlush(attempt);
        verify(evaluationMock).evaluate(attempt);
        verify(answers, never()).saveAndFlush(any());
    }

    @Test
    void lateManualSubmissionBecomesAutoSubmittedWithoutSecondResult() {
        StudentExamService concurrentService = serviceWithMockedExpiration();
        when(attempts.findExamIdByIdAndStudentId(22L, 7L))
                .thenReturn(Optional.of(1L));
        when(attempts.findByIdAndStudentIdForUpdate(22L, 7L))
                .thenReturn(Optional.of(attempt));
        when(attempt.getId()).thenReturn(22L);
        when(attempt.getExamId()).thenReturn(1L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        when(attempt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(1));
        when(exams.findById(1L)).thenReturn(Optional.of(exam));
        when(exam.getTitle()).thenReturn("Exam");
        when(results.findByAttemptId(22L)).thenReturn(Optional.of(mock(ExamResult.class)));
        doAnswer(invocation -> {
            when(attempt.getStatus()).thenReturn(AttemptStatus.AUTO_SUBMITTED);
            return null;
        }).when(attempt).setStatus(any());

        var result = concurrentService.submit("student@example.com", 22L);

        assertEquals(AttemptStatus.AUTO_SUBMITTED, result.status());
        verify(evaluationMock).evaluate(attempt);
        verify(results, never()).saveAndFlush(any());
    }

    private StudentExamService serviceWithMockedExpiration() {
        when(expirationMock.expireIfNeeded(anyLong(), anyLong())).thenReturn(null);
        return new StudentExamService(
                users, exams, attempts, questions, options, answers, results,
                evaluationMock, expirationMock);
    }

    private void setupPublishedExam() {
        when(exams.findByIdForUpdate(1L)).thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(1L);
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        when(exam.getStartsAt()).thenReturn(Instant.now().minusSeconds(10));
        when(exam.getEndsAt()).thenReturn(Instant.now().plusSeconds(1000));
        when(exam.getDurationMinutes()).thenReturn(30);
    }
}
