package com.hertz.hertz_be.global.sse;

import static com.hertz.hertz_be.global.util.AuthUtil.extractRefreshTokenFromCookie;

import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
            throw new BusinessException(
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
        }

        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        return sseService.subscribe(userId);
    }
}


