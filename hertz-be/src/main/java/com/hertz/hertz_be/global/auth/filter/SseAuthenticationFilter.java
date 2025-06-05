package com.hertz.hertz_be.global.auth.filter;

import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.hertz.hertz_be.global.util.AuthUtil.extractRefreshTokenFromCookie;

@Component
@RequiredArgsConstructor
public class SseAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        String requestURI = request.getRequestURI();

        if (acceptHeader != null && acceptHeader.contains("text/event-stream")
                && requestURI.startsWith("/api/sse/subscribe")) {

            String refreshToken = extractRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                throw new RefreshTokenInvalidException();
            }

            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenForSse(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

