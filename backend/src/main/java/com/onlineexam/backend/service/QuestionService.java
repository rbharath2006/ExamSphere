
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QuestionService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final UserAccountRepository userRepository;

    public QuestionService(
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository,
            UserAccountRepository userRepository
    ) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuestionResponse addQuestion(
            String teacherEmail,
            Long examId,
            CreateQuestionRequest request
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

        // 2. Verify Teacher role and account status.
        if (teacher.getRole() != UserRole.TEACHER
                || !teacher.isEnabled()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only enabled Teachers can add questions"
            );
        }

        // 3. Verify that this examination belongs to the Teacher.
        Exam exam = examRepository
                .findByIdAndCreatedById(examId, teacher.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Examination not found"
                        )
                );

        // 4. Questions can only be added to draft exams.
        if (exam.getStatus() != ExamStatus.DRAFT) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Questions can only be added to draft examinations"
            );
        }

        // 5. Validate the answer options.
        validateOptions(request);

        // 6. Validate negative marking.
        BigDecimal negativeMarks =
                request.negativeMarks() == null
                        ? BigDecimal.ZERO
                        : request.negativeMarks();

        if (negativeMarks.compareTo(request.marks()) > 0) {

            throw new IllegalArgumentException(
                    "Negative marks cannot exceed question marks"
            );
        }

        // 7. Calculate the next question order.
        long existingQuestionCount =
                questionRepository.countByExamId(examId);

        int displayOrder =
                Math.toIntExact(existingQuestionCount + 1);

        // 8. Create and save the question.
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

        // 9. Create and save the answer options.
        List<QuestionOption> options = new java.util.ArrayList<>();

        int optionOrder = 1;

        for (CreateQuestionOptionRequest optionRequest : request.options()) {

            QuestionOption option = new QuestionOption(
                    savedQuestion.getId(),
                    optionRequest.optionText().trim(),
                    optionOrder++,
                    optionRequest.isCorrect()
            );

            options.add(option);
        }

        List<QuestionOption> savedOptions =
                optionRepository.saveAllAndFlush(options);

        // 10. Update examination maximum marks.
        BigDecimal updatedTotalMarks =
                exam.getTotalMarks().add(request.marks());

        exam.setTotalMarks(updatedTotalMarks);

        examRepository.save(exam);

        // 11. Return the saved question.
        return QuestionResponse.from(
                savedQuestion,
                savedOptions
        );
    }

    /**
     * Validates question option count and correct-answer selection.
     */
    private void validateOptions(CreateQuestionRequest request) {

        List<CreateQuestionOptionRequest> options =
                request.options();

        if (options == null) {
            throw new IllegalArgumentException(
                    "Question options are required"
            );
        }

        // Exactly one answer must be correct.
        long correctAnswerCount = options.stream()
                .filter(CreateQuestionOptionRequest::isCorrect)
                .count();

        if (correctAnswerCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one option must be marked as correct"
            );
        }

        // All option text must be unique.
        long uniqueOptionCount = options.stream()
                .map(option ->
                        option.optionText().trim().toLowerCase(Locale.ROOT)
                )
                .distinct()
                .count();

        if (uniqueOptionCount != options.size()) {
            throw new IllegalArgumentException(
                    "Duplicate answer options are not allowed"
            );
        }

        // Single-choice MCQ must have exactly four options.
        if (request.questionType() == QuestionType.SINGLE_CHOICE
                && options.size() != 4) {

            throw new IllegalArgumentException(
                    "Single-choice questions must have exactly four options"
            );
        }

        // True/False questions must have exactly True and False.
        if (request.questionType() == QuestionType.TRUE_FALSE) {

            if (options.size() != 2) {
                throw new IllegalArgumentException(
                        "True/False questions must have exactly two options"
                );
            }

            Set<String> optionTexts = options.stream()
                    .map(option ->
                            option.optionText().trim().toLowerCase(Locale.ROOT)
                    )
                    .collect(java.util.stream.Collectors.toSet());

            if (!optionTexts.equals(Set.of("true", "false"))) {
                throw new IllegalArgumentException(
                        "True/False options must be True and False"
                );
            }
        }
    }
}
