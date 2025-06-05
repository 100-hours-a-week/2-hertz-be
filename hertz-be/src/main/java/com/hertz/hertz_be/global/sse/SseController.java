package com.hertz.hertz_be.global.sse;

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

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId,
                                HttpServletRequest request,
                                HttpServletResponse response) {

        String origin = request.getHeader("Origin");

        List<String> allowedOrigins = List.of(
                "http://localhost:3000",
                "https://hertz-tuning.com",
                "https://dev.hertz-tuning.com",
                "https://local.hertz-tuning.com:3000"
        );

        if (origin == null || allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Headers", "Authorization");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return sseService.subscribe(userId);
    }
}

