package com.example.hotelreservation.security;

import com.example.hotelreservation.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RestSecurityHandlers {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> write(request, response, HttpStatus.UNAUTHORIZED,
                "Authentication is required or the credentials are invalid", true);
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action", false);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       String message, boolean challenge) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (challenge) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"hotel-reservation\"");
        }
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(LocalDateTime.now(),
                status.value(), status.getReasonPhrase(), message, request.getRequestURI(), List.of()));
    }
}
