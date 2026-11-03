
package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.CreateExamRequest;
import com.onlineexam.backend.dto.ExamResponse;
import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamRepository;
import com.onlineexam.backend.repository.QuestionOptionRepository;
import com.onlineexam.backend.repository.QuestionRepository;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final UserAccountRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public ExamService(
            ExamRepository examRepository,
            UserAccountRepository userRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository
    ) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    /**
     * Creates a new examination for the authenticated Teacher.
     */
    @Transactional
    public ExamResponse createExam(
            String teacherEmail,
            CreateExamRequest request
    ) {

        // 1. Find the authenticated Teacher.
        UserAccount teacher = userRepository
                .findByEmailIgnoreCase(teacherEmail.trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Teacher account not found"
                        )
                );

        // 2. Verify the account role and status.
        if (teacher.getRole() != UserRole.TEACHER
                || !teacher.isEnabled()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only enabled Teachers can create examinations"
            );
        }

        // 3. Validate examination schedule.
        if (!request.endsAt().isAfter(request.startsAt())) {

            throw new IllegalArgumentException(
                    "Exam end time must be after start time"
            );
        }

        // 4. Prepare optional text fields.
        String description = request.description();

        if (description != null && description.isBlank()) {
            description = null;
        }

        String instructions = request.instructions();

        if (instructions != null && instructions.isBlank()) {
            instructions = null;
        }

        // 5. Create the examination.
        Exam exam = new Exam(
                request.title().trim(),
                description,
                request.subject().trim(),
                instructions,
                request.durationMinutes(),
                request.startsAt(),
                request.endsAt(),
                teacher.getId()
        );

        // 6. Save the examination.
        // The entity initializes status as DRAFT and total marks as 0.
        Exam savedExam = examRepository.saveAndFlush(exam);

        // 7. Return the response.
        return ExamResponse.from(savedExam);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getExams(String teacherEmail) {
        UserAccount teacher = userRepository
                .findByEmailIgnoreCase(teacherEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Teacher account not found"
                ));

        if (teacher.getRole() != UserRole.TEACHER || !teacher.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only enabled Teachers can view examinations"
            );
        }

        return examRepository.findByCreatedById(
                        teacher.getId(),
                        org.springframework.data.domain.Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(ExamResponse::from)
                .toList();
    }

    @Transactional
    public ExamResponse publishExam(String teacherEmail, Long examId) {
        UserAccount teacher = getEnabledTeacher(
                teacherEmail,
                "Only enabled Teachers can publish examinations"
        );
        Exam exam = getOwnedExam(examId, teacher.getId());

        if (exam.getStatus() != com.onlineexam.backend.entity.ExamStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft examinations can be published"
            );
        }

        validatePublishableExam(exam);
        exam.setStatus(com.onlineexam.backend.entity.ExamStatus.PUBLISHED);
        return ExamResponse.from(examRepository.saveAndFlush(exam));
    }

    @Transactional
    public ExamResponse closeExam(String teacherEmail, Long examId) {
        UserAccount teacher = getEnabledTeacher(
                teacherEmail,
                "Only enabled Teachers can close examinations"
        );
        Exam exam = getOwnedExam(examId, teacher.getId());

        if (exam.getStatus() != com.onlineexam.backend.entity.ExamStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only published examinations can be closed"
            );
        }

        exam.setStatus(com.onlineexam.backend.entity.ExamStatus.CLOSED);
        return ExamResponse.from(examRepository.saveAndFlush(exam));
    }

    private UserAccount getEnabledTeacher(
            String teacherEmail,
            String forbiddenMessage
    ) {
        UserAccount teacher = userRepository
                .findByEmailIgnoreCase(teacherEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Teacher account not found"
                ));

        if (teacher.getRole() != UserRole.TEACHER || !teacher.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    forbiddenMessage
            );
        }

        return teacher;
    }

    private Exam getOwnedExam(Long examId, Long teacherId) {
        return examRepository.findByIdAndCreatedByIdForUpdate(examId, teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination not found"
                ));
    }

    private void validatePublishableExam(Exam exam) {
        Instant now = Instant.now();
        if (exam.getDurationMinutes() == null || exam.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException(
                    "Exam duration must be positive"
            );
        }
        if (exam.getStartsAt() == null || !exam.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Exam start time must be in the future"
            );
        }
        if (exam.getEndsAt() == null
                || exam.getStartsAt() == null
                || !exam.getEndsAt().isAfter(exam.getStartsAt())) {
            throw new IllegalArgumentException(
                    "Exam end time must be after start time"
            );
        }

        List<com.onlineexam.backend.entity.Question> questions =
                questionRepository.findByExamIdOrderByDisplayOrderAsc(exam.getId());
        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one question is required before publishing"
            );
        }

        BigDecimal totalMarks = BigDecimal.ZERO;
        for (com.onlineexam.backend.entity.Question question : questions) {
            validateQuestion(question);
            totalMarks = totalMarks.add(question.getMarks());
        }

        if (exam.getTotalMarks() == null
                || exam.getTotalMarks().compareTo(totalMarks) != 0) {
            throw new IllegalArgumentException(
                    "Examination total marks do not match its questions"
            );
        }
    }

    private void validateQuestion(
            com.onlineexam.backend.entity.Question question
    ) {
        if (question.getMarks() == null
                || question.getMarks().compareTo(BigDecimal.ZERO) <= 0
                || question.getNegativeMarks() == null
                || question.getNegativeMarks().compareTo(BigDecimal.ZERO) < 0
                || question.getNegativeMarks().compareTo(question.getMarks()) > 0) {
            throw new IllegalArgumentException(
                    "Question marks are invalid"
            );
        }

        List<com.onlineexam.backend.entity.QuestionOption> options =
                optionRepository.findByQuestionIdOrderByDisplayOrderAsc(
                        question.getId()
                );
        if (options.isEmpty()) {
            throw new IllegalArgumentException(
                    "Every question must have answer options"
            );
        }
        if (options.stream()
                .filter(com.onlineexam.backend.entity.QuestionOption::isCorrect)
                .count() != 1) {
            throw new IllegalArgumentException(
                    "Every question must have exactly one correct answer"
            );
        }

        Set<String> uniqueTexts = new HashSet<>();
        for (com.onlineexam.backend.entity.QuestionOption option : options) {
            if (option.getOptionText() == null
                    || option.getOptionText().isBlank()
                    || !uniqueTexts.add(option.getOptionText()
                            .trim()
                            .toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "Question options must be non-empty and unique"
                );
            }
        }

        if (question.getQuestionType()
                == com.onlineexam.backend.entity.QuestionType.SINGLE_CHOICE
                && options.size() != 4) {
            throw new IllegalArgumentException(
                    "Single-choice questions must have exactly four options"
            );
        }
        if (question.getQuestionType()
                == com.onlineexam.backend.entity.QuestionType.TRUE_FALSE) {
            Set<String> expected = Set.of("true", "false");
            Set<String> actual = options.stream()
                    .map(option -> option.getOptionText()
                            .trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            if (options.size() != 2 || !actual.equals(expected)) {
                throw new IllegalArgumentException(
                        "True/False questions must have True and False options"
                );
            }
        }
    }
}
