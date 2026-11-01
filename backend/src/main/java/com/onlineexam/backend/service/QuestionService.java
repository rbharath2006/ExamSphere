
package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.CreateQuestionRequest;
import com.onlineexam.backend.dto.CreateQuestionOptionRequest;
import com.onlineexam.backend.dto.QuestionResponse;

import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QuestionService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final UserAccountRepository userRepository;
    private final ExamAttemptRepository attemptRepository;

    public QuestionService(
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository,
            UserAccountRepository userRepository,
            ExamAttemptRepository attemptRepository
    ) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Add a question to a Teacher's draft examination.
     */
    @Transactional
    public QuestionResponse addQuestion(
            String teacherEmail,
            Long examId,
            CreateQuestionRequest request
    ) {

        UserAccount teacher = getEnabledTeacher(teacherEmail);

        Exam exam = getOwnedExam(examId, teacher.getId());

        ensureQuestionsCanBeModified(exam);

        validateOptions(request);

        BigDecimal negativeMarks =
                request.negativeMarks() == null
                        ? BigDecimal.ZERO
                        : request.negativeMarks();

        if (negativeMarks.compareTo(request.marks()) > 0) {
            throw new IllegalArgumentException(
                    "Negative marks cannot exceed question marks"
            );
        }

        long existingCount =
                questionRepository.countByExamId(examId);

        int displayOrder = Math.toIntExact(existingCount + 1);

        Question question = new Question(
                examId,
                request.questionText().trim(),
                request.questionType(),
                request.marks(),
                negativeMarks,
                displayOrder
        );

        Question savedQuestion =
                questionRepository.saveAndFlush(question);

        List<QuestionOption> savedOptions =
                saveOptions(savedQuestion.getId(), request);

        // Recalculate the examination's total marks.
        exam.setTotalMarks(
                exam.getTotalMarks().add(request.marks())
        );

        examRepository.saveAndFlush(exam);

        return QuestionResponse.from(
                savedQuestion,
                savedOptions
        );
    }

    /**
     * List all questions belonging to an examination.
     * Only the owning Teacher can access this endpoint.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(
            String teacherEmail,
            Long examId
    ) {

        UserAccount teacher = getEnabledTeacher(teacherEmail);

        getOwnedExam(examId, teacher.getId());

        List<Question> questions =
                questionRepository.findByExamIdOrderByDisplayOrderAsc(
                        examId
                );

        List<QuestionResponse> responses = new ArrayList<>();

        for (Question question : questions) {

            List<QuestionOption> options =
                    optionRepository
                            .findByQuestionIdOrderByDisplayOrderAsc(
                                    question.getId()
                            );

            responses.add(
                    QuestionResponse.from(question, options)
            );
        }

        return responses;
    }

    /**
     * Edit an existing question and replace its options.
     */
    @Transactional
    public QuestionResponse updateQuestion(
            String teacherEmail,
            Long examId,
            Long questionId,
            CreateQuestionRequest request
    ) {

        UserAccount teacher = getEnabledTeacher(teacherEmail);

        Exam exam = getOwnedExam(examId, teacher.getId());

        ensureQuestionsCanBeModified(exam);

        Question question = questionRepository
                .findByIdAndExamId(questionId, examId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Question not found"
                        )
                );

        validateOptions(request);

        BigDecimal negativeMarks =
                request.negativeMarks() == null
                        ? BigDecimal.ZERO
                        : request.negativeMarks();

        if (negativeMarks.compareTo(request.marks()) > 0) {
            throw new IllegalArgumentException(
                    "Negative marks cannot exceed question marks"
            );
        }

        // Remember the previous marks before updating.
        BigDecimal previousMarks = question.getMarks();

        // Update question information.
        question.setQuestionText(request.questionText().trim());
        question.setQuestionType(request.questionType());
        question.setMarks(request.marks());
        question.setNegativeMarks(negativeMarks);

        Question updatedQuestion =
                questionRepository.saveAndFlush(question);

        // Replace the old options with the new options.
        List<QuestionOption> oldOptions =
                optionRepository.findByQuestionIdOrderByDisplayOrderAsc(
                        questionId
                );

        optionRepository.deleteAllInBatch(oldOptions);
        optionRepository.flush();

        List<QuestionOption> updatedOptions =
                saveOptions(questionId, request);

        // Recalculate total marks.
        BigDecimal updatedTotal =
                exam.getTotalMarks()
                        .subtract(previousMarks)
                        .add(request.marks());

        exam.setTotalMarks(updatedTotal);

        examRepository.saveAndFlush(exam);

        return QuestionResponse.from(
                updatedQuestion,
                updatedOptions
        );
    }

    /**
     * Delete a question and recalculate examination marks.
     */
    @Transactional
    public void deleteQuestion(
            String teacherEmail,
            Long examId,
            Long questionId
    ) {

        UserAccount teacher = getEnabledTeacher(teacherEmail);

        Exam exam = getOwnedExam(examId, teacher.getId());

        ensureQuestionsCanBeModified(exam);

        Question question = questionRepository
                .findByIdAndExamId(questionId, examId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Question not found"
                        )
                );

        BigDecimal questionMarks = question.getMarks();

        // Delete the question.
        // Database cascade deletes its question options.
        questionRepository.delete(question);
        questionRepository.flush();

        // Recalculate total examination marks.
        exam.setTotalMarks(
                exam.getTotalMarks().subtract(questionMarks)
        );

        examRepository.saveAndFlush(exam);

        // Reassign question display order without gaps.
        compactDisplayOrder(examId);
    }

    /**
     * Find an enabled Teacher.
     */
    private UserAccount getEnabledTeacher(String email) {

        UserAccount teacher = userRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Teacher account not found"
                        )
                );

        if (teacher.getRole() != UserRole.TEACHER
                || !teacher.isEnabled()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only enabled Teachers can manage questions"
            );
        }

        return teacher;
    }

    /**
     * Verify examination ownership.
     */
    private Exam getOwnedExam(Long examId, Long teacherId) {

        return examRepository
                .findByIdAndCreatedById(examId, teacherId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Examination not found"
                        )
                );
    }

    /**
     * Prevent changes to published exams or exams with attempts.
     */
    private void ensureQuestionsCanBeModified(Exam exam) {

        if (exam.getStatus() != ExamStatus.DRAFT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Questions can only be changed in draft examinations"
            );
        }

        if (attemptRepository.existsByExamId(exam.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Questions cannot be changed after an exam attempt exists"
            );
        }
    }

    /**
     * Save answer options for a question.
     */
    private List<QuestionOption> saveOptions(
            Long questionId,
            CreateQuestionRequest request
    ) {

        List<QuestionOption> options = new ArrayList<>();

        int order = 1;

        for (CreateQuestionOptionRequest optionRequest : request.options()) {

            QuestionOption option = new QuestionOption(
                    questionId,
                    optionRequest.optionText().trim(),
                    order++,
                    optionRequest.isCorrect()
            );

            options.add(option);
        }

        return optionRepository.saveAllAndFlush(options);
    }

    /**
     * Validate answer options.
     */
    private void validateOptions(CreateQuestionRequest request) {

        List<CreateQuestionOptionRequest> options =
                request.options();

        if (options == null) {
            throw new IllegalArgumentException(
                    "Question options are required"
            );
        }

        long correctCount = options.stream()
                .filter(CreateQuestionOptionRequest::isCorrect)
                .count();

        if (correctCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one option must be marked as correct"
            );
        }

        long uniqueCount = options.stream()
                .map(option ->
                        option.optionText()
                                .trim()
                                .toLowerCase(Locale.ROOT)
                )
                .distinct()
                .count();

        if (uniqueCount != options.size()) {
            throw new IllegalArgumentException(
                    "Duplicate answer options are not allowed"
            );
        }

        if (request.questionType() == QuestionType.SINGLE_CHOICE
                && options.size() != 4) {

            throw new IllegalArgumentException(
                    "Single-choice questions must have exactly four options"
            );
        }

        if (request.questionType() == QuestionType.TRUE_FALSE) {

            if (options.size() != 2) {
                throw new IllegalArgumentException(
                        "True/False questions must have exactly two options"
                );
            }

            Set<String> optionTexts = options.stream()
                    .map(option ->
                            option.optionText()
                                    .trim()
                                    .toLowerCase(Locale.ROOT)
                    )
                    .collect(java.util.stream.Collectors.toSet());

            if (!optionTexts.equals(Set.of("true", "false"))) {
                throw new IllegalArgumentException(
                        "True/False options must be True and False"
                );
            }
        }
    }

    /**
     * Compact question ordering after a deletion.
     *
     * Temporary higher positions avoid conflicts with the
     * unique constraint on (exam_id, display_order).
     */
    private void compactDisplayOrder(Long examId) {

        List<Question> remaining =
                questionRepository.findByExamIdOrderByDisplayOrderAsc(
                        examId
                );

        if (remaining.isEmpty()) {
            return;
        }

        int offset = remaining.get(
                remaining.size() - 1
        ).getDisplayOrder() + 1;

        // Move existing questions to temporary unique positions.
        for (Question question : remaining) {
            question.setDisplayOrder(
                    Math.addExact(question.getDisplayOrder(), offset)
            );
        }

        questionRepository.saveAllAndFlush(remaining);

        // Assign their final sequential positions.
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setDisplayOrder(i + 1);
        }

        questionRepository.saveAllAndFlush(remaining);
    }
}
