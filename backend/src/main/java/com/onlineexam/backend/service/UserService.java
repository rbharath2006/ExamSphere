
package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.exception.EmailAlreadyRegisteredException;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public UserAccount registerStudent(
            String fullName,
            String email,
            String rawPassword
    ) {

        // Validate essential input
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        // Normalize email
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        // Check duplicate email
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(
                    "An account with this email already exists"
            );
        }

        // Encode password
        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        // Public registration can only create STUDENT accounts
        UserAccount newUser = new UserAccount(
                fullName,
                normalizedEmail,
                encodedPassword,
                UserRole.STUDENT
        );

        // Save user
        return userRepository.save(newUser);
    }
}
