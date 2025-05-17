package com.hertz.hertz_be.global;

import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestLoginController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;

    public TestLoginController(JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "사용자 Id로 AT를 반환하는 API", description = "회원가입 안된 임의의 사용자의 Id도 사용 가능")
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

    @GetMapping("/ping")
    @Operation(summary = "서버 헬스체크를 위한 API")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
