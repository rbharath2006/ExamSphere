
package com.onlineexam.backend.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.onlineexam.backend.repository.UserAccountRepository;
import com.onlineexam.backend.security.AccountStatusFilter;

import org.springframework.security.web.access.intercept.AuthorizationFilter;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin
    ) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(allowedOrigin)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "Authorization",
                        "X-XSRF-TOKEN"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }


    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            UserAccountRepository userRepository
    ) throws Exception {

        http
                .addFilterBefore(
                        new AccountStatusFilter(userRepository),
                        AuthorizationFilter.class
                )

                .authenticationProvider(authenticationProvider)
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.spa())


                .authorizeHttpRequests(authorize -> authorize

                        // Public endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health",
                                "/api/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // Administrator permissions
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Teacher permissions
                        .requestMatchers("/api/teacher/**")
                        .hasRole("TEACHER")

                        // Student permissions
                        .requestMatchers("/api/student/**")
                        .hasRole("STUDENT")

                        // All remaining endpoints require authentication
                        .anyRequest().authenticated()
                )


                // Database-backed session authentication
                .formLogin(form -> form

                        .loginProcessingUrl("/api/auth/login")

                        .usernameParameter("email")
                        .passwordParameter("password")

                        .successHandler((request, response, authentication) -> {

                            response.setStatus(
                                    HttpServletResponse.SC_OK
                            );

                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );

                            response.getWriter().write(
                                    "{\"message\":\"Login successful\"}"
                            );
                        })

                        .failureHandler((request, response, exception) -> {

                            response.setStatus(
                                    HttpServletResponse.SC_UNAUTHORIZED
                            );

                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );

                            response.getWriter().write(
                                    "{\"error\":\"Invalid email or password\"}"
                            );
                        })
                )

                // Secure logout
                .logout(logout -> logout

                        .logoutUrl("/api/auth/logout")

                        .invalidateHttpSession(true)

                        .clearAuthentication(true)

                        .deleteCookies("JSESSIONID")

                        .logoutSuccessHandler((request, response, authentication) -> {

                            response.setStatus(
                                    HttpServletResponse.SC_OK
                            );

                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );

                            response.getWriter().write(
                                    "{\"message\":\"Logout successful\"}"
                            );
                        })
                )

                // APIs return HTTP 401 instead of redirecting to HTML login
                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint((request, response, authException) -> {

                            response.setStatus(
                                    HttpServletResponse.SC_UNAUTHORIZED
                            );

                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );

                            response.getWriter().write(
                                    "{\"error\":\"Authentication required\"}"
                            );
                        })
                );

        return http.build();
    }
}
