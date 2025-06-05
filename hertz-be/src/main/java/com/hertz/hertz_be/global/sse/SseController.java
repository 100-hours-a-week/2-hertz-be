package com.hertz.hertz_be.global.sse;

import static com.hertz.hertz_be.global.util.AuthUtil.extractRefreshTokenFromCookie;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
@Tag(name = "SSE 연결 API")
public class SseController {

    private final SseService sseService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(HttpServletRequest request) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new RefreshTokenInvalidException();
        }

        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        return sseService.subscribe(userId);
    }
}


