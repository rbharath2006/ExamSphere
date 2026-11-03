package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.*;
import com.onlineexam.backend.entity.*;
import com.onlineexam.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Clock;
import java.util.*;

@Service
public class StudentExamService {
    private final UserAccountRepository users;
    private final ExamRepository exams;
    private final ExamAttemptRepository attempts;
    private final QuestionRepository questions;
    private final QuestionOptionRepository options;
    private final StudentAnswerRepository answers;
    private final ExamResultRepository results;
    private final ExamAttemptEvaluationService evaluation;
    private final ExpiredAttemptService expiration;
    private final Clock clock;

    public StudentExamService(
            UserAccountRepository users, ExamRepository exams,
            ExamAttemptRepository attempts, QuestionRepository questions,
            QuestionOptionRepository options, StudentAnswerRepository answers,
            ExamResultRepository results
    ) {
        this(users, exams, attempts, questions, options, answers, results,
                new ExamAttemptEvaluationService(
                        exams, questions, options, answers, results),
                new ExpiredAttemptService(
                        attempts,
                        new ExamAttemptEvaluationService(
                                exams, questions, options, answers, results)),
                Clock.systemUTC());
    }

    @Autowired
    public StudentExamService(
            UserAccountRepository users, ExamRepository exams,
            ExamAttemptRepository attempts, QuestionRepository questions,
            QuestionOptionRepository options, StudentAnswerRepository answers,
            ExamResultRepository results,
            ExamAttemptEvaluationService evaluation,
            ExpiredAttemptService expiration,
            Clock clock
    ) {
        this.users = users;
        this.exams = exams;
        this.attempts = attempts;
        this.questions = questions;
        this.options = options;
        this.answers = answers;
        this.results = results;
        this.evaluation = evaluation;
        this.expiration = expiration;
        this.clock = clock;
    }

    public StudentExamService(
            UserAccountRepository users, ExamRepository exams,
            ExamAttemptRepository attempts, QuestionRepository questions,
            QuestionOptionRepository options, StudentAnswerRepository answers,
            ExamResultRepository results,
            ExamAttemptEvaluationService evaluation,
            ExpiredAttemptService expiration
    ) {
        this(users, exams, attempts, questions, options, answers, results,
                evaluation, expiration, Clock.systemUTC());
    }

    @Transactional(readOnly = true)
    public List<StudentExamResponse> listExams(String email) {
        UserAccount student = student(email);
        Instant now = clock.instant();
        return exams.findByStatusInOrderByStartsAtAsc(
                        List.of(ExamStatus.PUBLISHED, ExamStatus.CLOSED))
                .stream().map(exam -> {
                    Optional<ExamAttempt> attempt =
                            attempts.findByExamIdAndStudentId(
                                    exam.getId(), student.getId());
                    if (exam.getStatus() == ExamStatus.CLOSED
                            && attempt.isEmpty()) {
                        return null;
                    }
                    return new StudentExamResponse(
                            exam.getId(), exam.getTitle(), exam.getSubject(),
                            exam.getInstructions(), exam.getDurationMinutes(),
                            exam.getStartsAt(), exam.getEndsAt(),
                            exam.getStatus(),
                            exam.getTotalMarks(),
                            exam.getStatus() == ExamStatus.PUBLISHED
                                    && exam.getStartsAt().isAfter(now),
                            exam.getStatus() == ExamStatus.PUBLISHED
                                    && !exam.getStartsAt().isAfter(now)
                                    && exam.getEndsAt().isAfter(now),
                            attempt.isPresent(), attempt.map(ExamAttempt::getId).orElse(null)
                    );
                }).filter(Objects::nonNull).toList();
    }

    @Transactional
    public StudentAttemptResponse start(String email, Long examId) {
        UserAccount student = student(email);
        // Lock order: exam first, then the student's attempt. This serializes
        // starts with Teacher close operations without lock-order inversion.
        Exam exam = exams.findByIdForUpdate(examId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Examination not found"));
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Examination is not available");
        }
        Optional<Long> existingId =
                attempts.findIdByExamIdAndStudentId(examId, student.getId());
        if (existingId.isPresent()) {
            applyExpiration(existingId.get(), student.getId());
            ExamAttempt attempt = attempts.findByExamIdAndStudentIdForUpdate(
                    examId, student.getId()).orElseThrow();
            boolean expired = expireLockedAttemptIfNeeded(attempt);
            if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
                return attemptResponse(attempt, exam);
            }
            if (expired) {
                return attemptResponse(attempt, exam);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This examination attempt has already been submitted");
        }
        Instant now = clock.instant();
        if (now.isBefore(exam.getStartsAt()) || !now.isBefore(exam.getEndsAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Examination is outside its scheduled time window");
        }
        Instant deadline = now.plusSeconds(exam.getDurationMinutes() * 60L);
        if (exam.getEndsAt().isBefore(deadline)) deadline = exam.getEndsAt();
        ExamAttempt attempt;
        try {
            attempt = attempts.saveAndFlush(
                    new ExamAttempt(examId, student.getId(), deadline));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This examination attempt already exists", exception);
        }
        return attemptResponse(attempt, exam);
    }

    @Transactional
    public StudentAttemptResponse getAttempt(String email, Long attemptId) {
        UserAccount student = student(email);
        Long examId = attempts.findExamIdByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));
        Exam exam = exams.findById(examId).orElseThrow();
        applyExpiration(attemptId, student.getId());
        ExamAttempt attempt = attempts.findByIdAndStudentIdForUpdate(
                attemptId, student.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        expireLockedAttemptIfNeeded(attempt);
        return attemptResponse(attempt, exam);
    }

    @Transactional
    public boolean saveAnswer(String email, Long attemptId, SaveAnswerRequest request) {
        UserAccount student = student(email);
        Long examId = attempts.findExamIdByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));
        // Lock order: discover attempt without a lock, then exam, expire in a
        // separate transaction if needed, then lock the attempt.
        // Teacher close also locks exam first; this avoids an exam/attempt cycle.
        Exam exam = exams.findByIdForUpdate(examId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Examination not found"));
        applyExpiration(attemptId, student.getId());
        ExamAttempt attempt = attempts.findByIdAndStudentIdForUpdate(
                attemptId, student.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Attempt not found"));
        if (expireLockedAttemptIfNeeded(attempt)) {
            return false;
        }
        if (!attempt.getExamId().equals(exam.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attempt does not belong to this examination");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This attempt is no longer accepting answers");
        }
        if (exam.getStatus() == ExamStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This examination is closed and no longer accepts answers");
        }
        Question question = questions.findByIdAndExamId(
                request.questionId(), attempt.getExamId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question does not belong to this examination"));
        options.findByIdAndQuestionId(request.selectedOptionId(), question.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected option does not belong to this question"));
        StudentAnswer answer = answers.findByAttemptIdAndQuestionId(
                attemptId, question.getId()).orElse(null);
        if (answer == null) answers.saveAndFlush(
                new StudentAnswer(attemptId, question.getId(), request.selectedOptionId()));
        else {
            answer.setSelectedOptionId(request.selectedOptionId());
            answers.saveAndFlush(answer);
        }
        return true;
    }

    @Transactional
    public StudentResultResponse submit(String email, Long attemptId) {
        UserAccount student = student(email);
        Long examId = attempts.findExamIdByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));
        applyExpiration(attemptId, student.getId());
        ExamAttempt attempt = attempts.findByIdAndStudentIdForUpdate(
                attemptId, student.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        expireLockedAttemptIfNeeded(attempt);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return resultResponse(attempt, false);
        }
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(clock.instant());
        attempts.saveAndFlush(attempt);
        evaluation.evaluate(attempt);
        return resultResponse(attempt, false);
    }

    @Transactional
    public StudentResultResponse result(String email, Long attemptId) {
        UserAccount student = student(email);
        Long examId = attempts.findExamIdByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));
        applyExpiration(attemptId, student.getId());
        ExamAttempt attempt = attempts.findByIdAndStudentIdForUpdate(
                attemptId, student.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        expireLockedAttemptIfNeeded(attempt);
        return resultResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public boolean resultsPublished(String email, Long attemptId) {
        UserAccount student = student(email);
        Long examId = attempts.findExamIdByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));
        Exam exam = exams.findById(examId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Examination not found"));
        return exam.getResultsPublishedAt() != null;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void autoSubmitExpiredAttempts() {
        attempts.findByStatusAndExpiresAtBeforeForUpdate(
                AttemptStatus.IN_PROGRESS, clock.instant()
        ).forEach(attempt -> {
            attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
            attempt.setSubmittedAt(clock.instant());
            attempts.saveAndFlush(attempt);
            evaluation.evaluate(attempt);
        });
    }

    private void applyExpiration(Long attemptId, Long studentId) {
        expiration.expireIfNeeded(attemptId, studentId);
    }

    private boolean expireLockedAttemptIfNeeded(ExamAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS
                || clock.instant().isBefore(attempt.getExpiresAt())) {
            return false;
        }
        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        attempt.setSubmittedAt(clock.instant());
        attempts.saveAndFlush(attempt);
        evaluation.evaluate(attempt);
        return true;
    }

    private StudentAttemptResponse attemptResponse(ExamAttempt attempt, Exam exam) {
        List<Question> list = questions.findByExamIdOrderByDisplayOrderAsc(exam.getId());
        Map<Long, Long> selected = new HashMap<>();
        answers.findByAttemptId(attempt.getId())
                .forEach(answer -> selected.put(answer.getQuestionId(), answer.getSelectedOptionId()));
        List<StudentQuestionResponse> response = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Question question = list.get(i);
            List<StudentOptionResponse> optionResponse = options
                    .findByQuestionIdOrderByDisplayOrderAsc(question.getId())
                    .stream().map(option -> new StudentOptionResponse(
                            option.getId(), option.getOptionText())).toList();
            response.add(new StudentQuestionResponse(
                    question.getId(), i + 1, question.getQuestionText(),
                    question.getMarks(), question.getNegativeMarks(),
                    optionResponse, selected.get(question.getId())));
        }
        return new StudentAttemptResponse(
                attempt.getId(), exam.getId(), exam.getTitle(),
                exam.getDurationMinutes(), attempt.getStartedAt(),
                attempt.getExpiresAt(), attempt.getStatus(),
                exam.getStatus() == ExamStatus.PUBLISHED
                        && attempt.getStatus() == AttemptStatus.IN_PROGRESS
                        && clock.instant().isBefore(attempt.getExpiresAt()),
                response);
    }

    private StudentResultResponse resultResponse(
            ExamAttempt attempt,
            boolean requirePublication
    ) {
        Exam exam = exams.findById(attempt.getExamId()).orElseThrow();
        if (requirePublication && exam.getResultsPublishedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Exam results have not been published yet");
        }
        ExamResult result = results.findByAttemptId(attempt.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Result is not available yet"));
        return new StudentResultResponse(
                attempt.getId(), exam.getTitle(), result.getTotalMarks(),
                result.getObtainedMarks(), result.getPercentage(),
                attempt.getStatus(), attempt.getSubmittedAt());
    }

    private ExamAttempt ownedAttempt(String email, Long attemptId, boolean forUpdate) {
        UserAccount student = student(email);
        Optional<ExamAttempt> attempt = forUpdate
                ? attempts.findByIdAndStudentIdForUpdate(attemptId, student.getId())
                : attempts.findByIdAndStudentId(attemptId, student.getId());
        return attempt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
    }

    private UserAccount student(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student account not found"));
        if (user.getRole() != UserRole.STUDENT || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only enabled Students can access examinations");
        }
        return user;
    }
}
