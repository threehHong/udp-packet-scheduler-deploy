package com.lab.ups.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class ApiSecurityConfig {

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http, @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/transmission/start", "/api/transmission/stop", "/api/transmission/status", "/api/transmission/stream").permitAll()
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors
                        .configurationSource(corsSource)  // CORS 허용 설정
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
