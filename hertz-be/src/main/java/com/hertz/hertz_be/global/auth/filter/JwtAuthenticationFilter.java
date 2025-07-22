package com.hertz.hertz_be.global.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/v1/auth/token",
            "/api/ping",
            "/api/v1/users",
            "/api/v3/users",
            "/api/v1/nickname",
            "/swagger-ui.html",
            "/api/sse/subscribe"
    );

    private static final List<String> EXCLUDE_PREFIXES = List.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/api/v1/oauth/",
            "/api/test/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isExcluded = EXCLUDE_PATHS.contains(path) ||
                EXCLUDE_PREFIXES.stream().anyMatch(path::startsWith);

        if (isExcluded) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request);

            if (token == null) {
                throw new IllegalStateException();
            }

            if (!jwtTokenProvider.validateToken(token)) {
                sendErrorResponse(response, AuthResponseCode.ACCESS_TOKEN_EXPIRED.getCode(),
                        AuthResponseCode.ACCESS_TOKEN_EXPIRED.getMessage(),
                        HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (IllegalStateException ex) {
            sendErrorResponse(response, AuthResponseCode.UNAUTHORIZED.getCode(),
                    AuthResponseCode.UNAUTHORIZED.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception ex) {
            sendErrorResponse(response, AuthResponseCode.ACCESS_TOKEN_EXPIRED.getCode(),
                    AuthResponseCode.ACCESS_TOKEN_EXPIRED.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String code, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("data", null);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
