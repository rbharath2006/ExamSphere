
package com.onlineexam.backend.service;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.repository.UserAccountRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepository;

    public CustomUserDetailsService(
            UserAccountRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        UserAccount account = userRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid email or password"
                        )
                );

        return User.builder()
                .username(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .disabled(!account.isEnabled())
                .build();
    }
}
