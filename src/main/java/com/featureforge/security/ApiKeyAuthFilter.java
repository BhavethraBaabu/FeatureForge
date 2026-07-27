package com.featureforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featureforge.exception.ErrorResponse;
import com.featureforge.exception.InvalidApiKeyException;
import com.featureforge.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gates /api/v1/sdk/** with a distinct auth scheme from the dashboard's JWT.
 * These paths are permitAll in SecurityConfig at the Spring Security layer —
 * this filter is the actual gate, resolving X-API-Key to a project id and
 * short-circuiting with 401 if it doesn't check out. Everything else passes
 * through untouched.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SDK_PATH_PREFIX = "/api/v1/sdk/";
    public static final String RESOLVED_PROJECT_ID_ATTR = "resolvedProjectId";

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(SDK_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(API_KEY_HEADER);

        try {
            UUID projectId = apiKeyService.resolveProjectId(rawKey);
            request.setAttribute(RESOLVED_PROJECT_ID_ATTR, projectId);
            filterChain.doFilter(request, response);
        } catch (InvalidApiKeyException e) {
            writeUnauthorized(response, request.getRequestURI(), e.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", message, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
