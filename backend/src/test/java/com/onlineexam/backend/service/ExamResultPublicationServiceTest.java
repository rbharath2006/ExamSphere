package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.AttemptStatus;
import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamAttemptRepository;
import com.onlineexam.backend.repository.ExamRepository;
import com.onlineexam.backend.repository.ExamResultRepository;
import com.onlineexam.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamResultPublicationServiceTest {
    private static final String EMAIL = "teacher@example.com";
    private static final Long EXAM_ID = 1L;
    private static final Instant PUBLICATION_TIME =
            Instant.parse("2026-11-03T12:00:00Z");

    @Mock UserAccountRepository users;
    @Mock ExamRepository exams;
    @Mock ExamAttemptRepository attempts;
    @Mock ExamResultRepository results;
    @Mock UserAccount teacher;
    @Mock Exam exam;
    @Mock com.onlineexam.backend.entity.ExamAttempt attempt;

    private ExamResultPublicationService service;

    @BeforeEach
    void setUp() {
        service = new ExamResultPublicationService(
                users, exams, attempts, results,
                Clock.fixed(PUBLICATION_TIME, ZoneOffset.UTC));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher));
        when(teacher.getId()).thenReturn(3L);
        when(teacher.getRole()).thenReturn(UserRole.TEACHER);
        when(teacher.isEnabled()).thenReturn(true);
        when(exams.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(EXAM_ID);
        when(exams.saveAndFlush(exam)).thenReturn(exam);
    }

    @Test
    void publishesClosedExamWithCompletedResults() {
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);
        when(exam.getResultsPublishedAt()).thenReturn(null);
        when(attempts.findByExamIdForUpdate(EXAM_ID)).thenReturn(List.of(attempt));
        when(attempt.getId()).thenReturn(10L);
        when(attempt.getStatus()).thenReturn(AttemptStatus.SUBMITTED);
        when(results.existsByAttemptId(10L)).thenReturn(true);
        doAnswer(invocation -> {
            when(exam.getResultsPublishedAt()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(exam).setResultsPublishedAt(any());

        service.publish(EMAIL, EXAM_ID);

        assertEquals(PUBLICATION_TIME, exam.getResultsPublishedAt());
    }

    @Test
    void rejectsDraftAndPublishedExams() {
        when(exam.getResultsPublishedAt()).thenReturn(null);
        when(exam.getStatus()).thenReturn(ExamStatus.DRAFT);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.publish(EMAIL, EXAM_ID)).getStatusCode().value());

        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.publish(EMAIL, EXAM_ID)).getStatusCode().value());
    }

    @Test
    void rejectsInProgressAndMissingCompletedResults() {
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);
        when(exam.getResultsPublishedAt()).thenReturn(null);
        when(attempts.findByExamIdForUpdate(EXAM_ID)).thenReturn(List.of(attempt));
        when(attempt.getStatus()).thenReturn(AttemptStatus.IN_PROGRESS);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.publish(EMAIL, EXAM_ID)).getStatusCode().value());

        when(attempt.getStatus()).thenReturn(AttemptStatus.AUTO_SUBMITTED);
        when(attempt.getId()).thenReturn(10L);
        when(results.existsByAttemptId(10L)).thenReturn(false);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.publish(EMAIL, EXAM_ID)).getStatusCode().value());
    }

    @Test
    void preservesExistingPublicationTimestamp() {
        Instant original = PUBLICATION_TIME.minusSeconds(30);
        when(exam.getResultsPublishedAt()).thenReturn(original);

        service.publish(EMAIL, EXAM_ID);

        assertEquals(original, exam.getResultsPublishedAt());
    }

    @Test
    void rejectsAnotherTeachersExam() {
        when(exams.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.publish(EMAIL, EXAM_ID)).getStatusCode().value());
    }
}
