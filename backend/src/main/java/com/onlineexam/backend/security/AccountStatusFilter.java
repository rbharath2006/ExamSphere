
package com.onlineexam.backend.security;

import com.onlineexam.backend.entity.UserAccount;
import com.onlineexam.backend.repository.UserAccountRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserAccountRepository userRepository;

    public AccountStatusFilter(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // Check only authenticated, non-anonymous users.
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String email = authentication.getName();

            // Verify the user's latest account status in PostgreSQL.
            boolean accountEnabled = userRepository
                    .findByEmailIgnoreCase(email)
                    .map(UserAccount::isEnabled)
                    .orElse(false);

            if (!accountEnabled) {

                // Invalidate the existing session.
                HttpSession session = request.getSession(false);

                if (session != null) {
                    session.invalidate();
                }

                // Remove the current authentication.
                SecurityContextHolder.clearContext();

                // Return HTTP 401.
                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                response.setContentType(
                        MediaType.APPLICATION_JSON_VALUE
                );

                response.setCharacterEncoding(
                        StandardCharsets.UTF_8.name()
                );

                response.getWriter().write(
                        "{\"error\":\"Account is disabled or no longer available\"}"
                );

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
