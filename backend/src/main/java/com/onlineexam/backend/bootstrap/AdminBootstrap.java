
package com.onlineexam.backend.bootstrap;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.entity.UserRole;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminBootstrap(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {

        String adminEmail =
                environment.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL");

        String adminPassword =
                environment.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD");

        String adminName =
                environment.getProperty(
                        "APP_BOOTSTRAP_ADMIN_NAME",
                        "ExamSphere Administrator"
                );

        boolean emailProvided =
                adminEmail != null && !adminEmail.isBlank();

        boolean passwordProvided =
                adminPassword != null && !adminPassword.isBlank();

        if (!emailProvided && !passwordProvided) {
            return;
        }

        if (!emailProvided || !passwordProvided) {
            throw new IllegalStateException(
                    "Both Admin bootstrap email and password must be supplied"
            );
        }

        if (adminPassword.length() < 12 ||
                adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Admin password must contain at least 12 characters " +
                            "and must not exceed 72 UTF-8 bytes"
            );
        }

        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(adminEmail.trim())) {
            throw new IllegalStateException(
                    "The configured Admin email already belongs to another account"
            );
        }

        UserAccount admin = new UserAccount(
                adminName,
                adminEmail,
                passwordEncoder.encode(adminPassword),
                UserRole.ADMIN
        );

        userRepository.saveAndFlush(admin);

        System.out.println(
                "ExamSphere initial Admin account provisioned successfully."
        );
    }
}
