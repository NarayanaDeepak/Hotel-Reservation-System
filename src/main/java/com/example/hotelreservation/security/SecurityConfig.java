package com.example.hotelreservation.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestSecurityHandlers handlers;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // stateless API, no browser session/cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // anyone can browse 
                        .requestMatchers(HttpMethod.GET, "/api/room-types/**", "/api/amenities/**", "/api/rooms/**").permitAll()
                        // only ADMIN may change 
                        .requestMatchers(HttpMethod.POST, "/api/room-types/**", "/api/amenities/**", "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/room-types/**", "/api/amenities/**", "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/room-types/**", "/api/amenities/**", "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/room-types/**", "/api/amenities/**", "/api/rooms/**").hasRole("ADMIN")
                        // operational endpoints
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // customers + reservations: authenticated; ownership enforced with @PreAuthorize
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(handlers.authenticationEntryPoint()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers.authenticationEntryPoint())
                        .accessDeniedHandler(handlers.accessDeniedHandler()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
