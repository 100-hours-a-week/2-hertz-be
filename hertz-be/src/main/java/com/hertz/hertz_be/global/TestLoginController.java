package com.hertz.hertz_be.global;

import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TestLoginController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;

    public TestLoginController(JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody TestLoginRequestDTO request,
                                   HttpServletResponse response) {
        Long userId = request.getUserId();  // 클라이언트가 userId를 보냈다고 가정

        // Access Token 발급
        String accessToken = jwtTokenProvider.createAccessToken(userId);

        // Refresh Token 발급
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // Redis에 Refresh Token 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken, 1209600L); // 14일 (초 단위)

        // Set-Cookie 수동 설정 (SameSite=None + Secure + HttpOnly)
        String cookieValue = String.format(
                "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                refreshToken, 1209600
        );
        response.setHeader("Set-Cookie", cookieValue);

        // Access Token은 JSON body로 반환
        return ResponseEntity.ok()
                .body(Map.of("accessToken", accessToken));
    }
}
