
package com.onlineexam.backend.service;

import com.onlineexam.backend.dto.CreateExamRequest;
import com.onlineexam.backend.dto.ExamResponse;
import com.onlineexam.backend.entity.Exam;
import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.ExamRepository;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final UserAccountRepository userRepository;

    public ExamService(
            ExamRepository examRepository,
            UserAccountRepository userRepository
    ) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
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
}
