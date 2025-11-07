package com.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development security config: allow anonymous GET to /api/books and static resources so
 * the frontend dev server or static index can load data without Basic auth prompts.
 */
@Configuration
@Profile("dev")
public class DevWebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Simpler dev behavior: permit all requests (no auth) so local frontend/dev server
        // can access APIs without popups or Basic auth. This is ONLY active under 'dev' profile.
        http
            .csrf().disable()
            .authorizeRequests().anyRequest().permitAll();

        // Optionally disable frame options for H2 console or similar tools in dev
        http.headers().frameOptions().disable();

        return http.build();
    }
}
