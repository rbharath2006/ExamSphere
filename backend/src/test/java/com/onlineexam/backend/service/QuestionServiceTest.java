package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.CreateQuestionOptionRequest;
import com.onlineexam.backend.dto.CreateQuestionRequest;
import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.ExamStatus;
import com.onlineexam.backend.entity.Question;
import com.onlineexam.backend.entity.QuestionOption;
import com.onlineexam.backend.entity.QuestionType;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamAttemptRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionServiceTest {

    private static final String TEACHER_EMAIL = "teacher@example.com";
    private static final Long EXAM_ID = 1L;
    private static final Long QUESTION_ID = 10L;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private UserAccount teacher;

    @Mock
    private Exam exam;

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(
                examRepository,
                questionRepository,
                optionRepository,
                userRepository,
                attemptRepository
        );

        when(userRepository.findByEmailIgnoreCase(TEACHER_EMAIL))
                .thenReturn(Optional.of(teacher));
        when(teacher.getId()).thenReturn(3L);
        when(teacher.getRole()).thenReturn(UserRole.TEACHER);
        when(teacher.isEnabled()).thenReturn(true);
        when(examRepository.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.of(exam));
        when(exam.getId()).thenReturn(EXAM_ID);
        when(exam.getStatus()).thenReturn(ExamStatus.DRAFT);
        doAnswer(invocation -> {
            when(exam.getTotalMarks()).thenReturn(invocation.getArgument(0));
            return null;
        }).when(exam).setTotalMarks(any());
        when(attemptRepository.existsByExamId(EXAM_ID)).thenReturn(false);
    }

    @Test
    void teacherCanListQuestionsInDisplayOrderWithOptions() {
        Question first = question(1L, 1, "First", "2.00");
        Question second = question(2L, 2, "Second", "3.00");
        QuestionOption firstOption =
                new QuestionOption(1L, "A", 1, true);

        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of(first, second));
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(firstOption));
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(2L))
                .thenReturn(List.of());
        when(examRepository.findByIdAndCreatedById(EXAM_ID, 3L))
                .thenReturn(Optional.of(exam));

        var responses = questionService.getQuestions(TEACHER_EMAIL, EXAM_ID);

        assertEquals(List.of(1, 2),
                responses.stream().map(response -> response.displayOrder()).toList());
        assertEquals("A", responses.getFirst().options().getFirst().optionText());
        verify(examRepository).findByIdAndCreatedById(EXAM_ID, 3L);
        verify(examRepository, never())
                .findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L);
    }

    @Test
    void teacherCanUpdateQuestionAndTotalIsRecomputed() {
        Question question = question(QUESTION_ID, 1, "Old", "2.00");
        QuestionOption oldOption =
                new QuestionOption(QUESTION_ID, "Old option", 1, true);
        QuestionOption newOption =
                new QuestionOption(QUESTION_ID, "New option", 1, true);
        CreateQuestionRequest request = request(
                "Updated",
                QuestionType.SINGLE_CHOICE,
                "3.50",
                "0.50",
                fourOptions()
        );

        when(questionRepository.findByIdAndExamId(QUESTION_ID, EXAM_ID))
                .thenReturn(Optional.of(question));
        when(questionRepository.saveAndFlush(question)).thenReturn(question);
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(QUESTION_ID))
                .thenReturn(List.of(oldOption));
        when(optionRepository.saveAllAndFlush(any())).thenReturn(List.of(newOption));
        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of(question));

        questionService.updateQuestion(
                TEACHER_EMAIL, EXAM_ID, QUESTION_ID, request
        );

        assertEquals(new BigDecimal("3.50"), question.getMarks());
        assertEquals(new BigDecimal("3.50"), exam.getTotalMarks());
        verify(optionRepository).deleteAllInBatch(List.of(oldOption));
        verify(examRepository).saveAndFlush(exam);
    }

    @Test
    void teacherCanDeleteQuestionAndRemainingOrdersAreCompacted() {
        Question deleted = question(QUESTION_ID, 1, "Deleted", "2.00");
        Question remaining = question(11L, 2, "Remaining", "3.00");

        when(questionRepository.findByIdAndExamId(QUESTION_ID, EXAM_ID))
                .thenReturn(Optional.of(deleted));
        when(questionRepository.findByExamIdOrderByDisplayOrderAsc(EXAM_ID))
                .thenReturn(List.of(remaining), List.of(remaining));

        questionService.deleteQuestion(TEACHER_EMAIL, EXAM_ID, QUESTION_ID);

        assertEquals(1, remaining.getDisplayOrder());
        assertEquals(new BigDecimal("3.00"), exam.getTotalMarks());
        verify(questionRepository).delete(deleted);
        verify(questionRepository, org.mockito.Mockito.times(2))
                .saveAllAndFlush(List.of(remaining));
    }

    @Test
    void anotherTeacherCannotAccessExam() {
        when(examRepository.findByIdAndCreatedByIdForUpdate(EXAM_ID, 3L))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> questionService.getQuestions(TEACHER_EMAIL, EXAM_ID));

        verify(questionRepository, never())
                .findByExamIdOrderByDisplayOrderAsc(EXAM_ID);
    }

    @Test
    void nonTeacherCannotManageQuestions() {
        when(teacher.getRole()).thenReturn(UserRole.STUDENT);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> questionService.getQuestions(TEACHER_EMAIL, EXAM_ID)
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void publishedExamCannotBeModified() {
        when(exam.getStatus()).thenReturn(ExamStatus.PUBLISHED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> questionService.deleteQuestion(
                        TEACHER_EMAIL, EXAM_ID, QUESTION_ID
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(questionRepository, never())
                .findByIdAndExamId(QUESTION_ID, EXAM_ID);
    }

    @Test
    void examWithAttemptCannotBeModified() {
        when(attemptRepository.existsByExamId(EXAM_ID)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> questionService.deleteQuestion(
                        TEACHER_EMAIL, EXAM_ID, QUESTION_ID
                )
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void invalidOptionCountAndMultipleCorrectAnswersAreRejected() {
        CreateQuestionRequest invalidCount = request(
                "Invalid",
                QuestionType.SINGLE_CHOICE,
                "2.00",
                "0.00",
                List.of(
                        new CreateQuestionOptionRequest("A", true),
                        new CreateQuestionOptionRequest("B", false)
                )
        );

        assertThrows(IllegalArgumentException.class,
                () -> questionService.addQuestion(
                        TEACHER_EMAIL, EXAM_ID, invalidCount));

        CreateQuestionRequest multipleCorrect = request(
                "Invalid",
                QuestionType.SINGLE_CHOICE,
                "2.00",
                "0.00",
                List.of(
                        new CreateQuestionOptionRequest("A", true),
                        new CreateQuestionOptionRequest("B", true),
                        new CreateQuestionOptionRequest("C", false),
                        new CreateQuestionOptionRequest("D", false)
                )
        );

        assertThrows(IllegalArgumentException.class,
                () -> questionService.addQuestion(
                        TEACHER_EMAIL, EXAM_ID, multipleCorrect));
        verify(questionRepository, never()).saveAndFlush(any());
    }

    @Test
    void negativeMarksCannotExceedMarks() {
        CreateQuestionRequest request = request(
                "Invalid marks",
                QuestionType.SINGLE_CHOICE,
                "1.00",
                "1.50",
                fourOptions()
        );

        assertThrows(IllegalArgumentException.class,
                () -> questionService.addQuestion(
                        TEACHER_EMAIL, EXAM_ID, request));
        verify(questionRepository, never()).saveAndFlush(any());
    }

    private static Question question(
            Long id,
            int displayOrder,
            String text,
            String marks
    ) {
        Question question = org.mockito.Mockito.mock(Question.class);
        int[] currentDisplayOrder = {displayOrder};
        BigDecimal[] currentMarks = {new BigDecimal(marks)};
        when(question.getId()).thenReturn(id);
        when(question.getExamId()).thenReturn(EXAM_ID);
        when(question.getDisplayOrder()).thenAnswer(invocation -> currentDisplayOrder[0]);
        when(question.getQuestionText()).thenReturn(text);
        when(question.getQuestionType()).thenReturn(QuestionType.SINGLE_CHOICE);
        when(question.getMarks()).thenAnswer(invocation -> currentMarks[0]);
        doAnswer(invocation -> {
            currentDisplayOrder[0] = invocation.getArgument(0);
            return null;
        }).when(question).setDisplayOrder(any());
        doAnswer(invocation -> {
            currentMarks[0] = invocation.getArgument(0);
            return null;
        }).when(question).setMarks(any());
        return question;
    }

    private static CreateQuestionRequest request(
            String text,
            QuestionType type,
            String marks,
            String negativeMarks,
            List<CreateQuestionOptionRequest> options
    ) {
        return new CreateQuestionRequest(
                text,
                type,
                new BigDecimal(marks),
                new BigDecimal(negativeMarks),
                options
        );
    }

    private static List<CreateQuestionOptionRequest> fourOptions() {
        return List.of(
                new CreateQuestionOptionRequest("A", true),
                new CreateQuestionOptionRequest("B", false),
                new CreateQuestionOptionRequest("C", false),
                new CreateQuestionOptionRequest("D", false)
        );
    }
}
