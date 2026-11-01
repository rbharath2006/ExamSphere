
package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.exception.EmailAlreadyRegisteredException;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Public student registration.
     * Users can only register themselves as STUDENT.
     */
    @Transactional
    public UserAccount registerStudent(
            String fullName,
            String email,
            String rawPassword
    ) {
        return createUser(
                fullName,
                email,
                rawPassword,
                UserRole.STUDENT
        );
    }

    /**
     * Admin-only teacher creation.
     * Only the AdminController should expose this operation.
     */
    @Transactional
    public UserAccount createTeacher(
            String fullName,
            String email,
            String rawPassword
    ) {
        return createUser(
                fullName,
                email,
                rawPassword,
                UserRole.TEACHER
        );
    }

    /**
     * Common method used to create user accounts.
     */
    private UserAccount createUser(
            String fullName,
            String email,
            String rawPassword,
            UserRole role
    ) {

        // 1. Validate full name
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required"
            );
        }

        // 2. Validate email
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        // 3. Validate password
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        // 4. Check BCrypt's maximum password input size
        int passwordBytes =
                rawPassword.getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Password is too long for the configured password encoder"
            );
        }

        // 5. Normalize user information
        String normalizedName = fullName.trim();

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        // 6. Check whether email is already registered
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(
                    "An account with this email already exists"
            );
        }

        // 7. Hash the password before storing it
        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        // 8. Create account with the specified role
        UserAccount newUser = new UserAccount(
                normalizedName,
                normalizedEmail,
                encodedPassword,
                role
        );

        // 9. Save account and handle duplicate-email race conditions
        try {
            return userRepository.saveAndFlush(newUser);

        } catch (DataIntegrityViolationException exception) {

            throw new EmailAlreadyRegisteredException(
                    "An account with this email already exists"
            );
        }
    }

    /**
     * Find an account using its email address.
     */
    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                    "User account not found"
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User account not found"
                        )
                );
    }
}
