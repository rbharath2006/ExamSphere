package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;
import com.onlineexam.backend.entity.Question;
import com.onlineexam.backend.entity.QuestionOption;
import com.onlineexam.backend.entity.QuestionType;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamRepository;
import com.onlineexam.backend.repository.QuestionOptionRepository;
import com.onlineexam.backend.repository.QuestionRepository;
import com.onlineexam.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamServiceTest {

    private static final String EMAIL = "teacher@example.com";
    private static final Long EXAM_ID = 1L;

    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository optionRepository;
    @Mock
    private UserAccount teacher;
    @Mock
    private Exam exam;

    private ExamService service;

    @BeforeEach
    void setUp() {
        service = new ExamService(
                examRepository,
                userRepository,
                questionRepository,
                optionRepository
        );
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(teacher));
        when(teacher.getId()).thenReturn(3L);
        when(teacher.getRole()).thenReturn(UserRole.TEACHER);
        when(teacher.isEnabled()).thenReturn(true);
        when(examRepository.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(EXAM_ID);
        when(exam.getStatus()).thenReturn(ExamStatus.DRAFT);
        when(exam.getDurationMinutes()).thenReturn(60);
        when(exam.getStartsAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(exam.getEndsAt()).thenReturn(Instant.now().plusSeconds(7200));
        when(exam.getTotalMarks()).thenReturn(new BigDecimal("2.00"));
        when(examRepository.saveAndFlush(exam)).thenReturn(exam);
    }

    @Test
    void publishesValidDraftExam() {
        Question question = validQuestion();
        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of(question));
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(
                        new QuestionOption(10L, "A", 1, true),
                        new QuestionOption(10L, "B", 2, false),
                        new QuestionOption(10L, "C", 3, false),
                        new QuestionOption(10L, "D", 4, false)
                ));
        doAnswer(invocation -> {
            when(exam.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(exam).setStatus(any());

        service.publishExam(EMAIL, EXAM_ID);

        assertEquals(ExamStatus.PUBLISHED, exam.getStatus());
    }

    @Test
    void rejectsPublishingWithoutQuestions() {
        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.publishExam(EMAIL, EXAM_ID)
        );

        assertEquals(
                "At least one question is required before publishing",
                exception.getMessage()
        );
    }

    @Test
    void rejectsAlreadyPublishedExam() {
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.publishExam(EMAIL, EXAM_ID)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void rejectsAnotherTeachersExam() {
        when(examRepository.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.publishExam(EMAIL, EXAM_ID)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsInvalidSchedule() {
        when(exam.getStartsAt()).thenReturn(Instant.now().minusSeconds(1));
        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.publishExam(EMAIL, EXAM_ID)
        );

        assertEquals("Exam start time must be in the future", exception.getMessage());
    }

    @Test
    void closesPublishedExam() {
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);
        doAnswer(invocation -> {
            when(exam.getStatus()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(exam).setStatus(any());

        service.closeExam(EMAIL, EXAM_ID);

        assertEquals(ExamStatus.CLOSED, exam.getStatus());
    }

    @Test
    void rejectsClosingClosedExam() {
        when(exam.getStatus()).thenReturn(ExamStatus.CLOSED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.closeExam(EMAIL, EXAM_ID)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void rejectsUnauthorizedUser() {
        when(teacher.getRole()).thenReturn(UserRole.STUDENT);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.publishExam(EMAIL, EXAM_ID)
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    private Question validQuestion() {
        Question question = org.mockito.Mockito.mock(Question.class);
        when(question.getId()).thenReturn(10L);
        when(question.getMarks()).thenReturn(new BigDecimal("2.00"));
        when(question.getNegativeMarks()).thenReturn(new BigDecimal("0.50"));
        when(question.getQuestionType()).thenReturn(QuestionType.SINGLE_CHOICE);
        return question;
    }
}
