package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDTO;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.auth.dto.request.TestLoginRequestDTO;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "Auth 관련 API")
public class AuthController {

    private final AuthService authTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;

    @Value("${is.local}")
    private boolean isLocal;

    @PostMapping("/v1/auth/token")
    @Operation(summary = "Access Token 재발급 API")
    public ResponseEntity<ResponseDto<ReissueAccessTokenResponseDTO>> reissueAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new RefreshTokenInvalidException();
        }

        Map.Entry<ReissueAccessTokenResponseDTO, String> result = authTokenService.reissueAccessToken(refreshToken);
        ReissueAccessTokenResponseDTO accessTokenResponse = result.getKey();
        String newRefreshToken = result.getValue();

        //ResponseCookie 설정 (환경에 따라 분기)
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .maxAge(1209600)
                .path("/")
                .sameSite("None")
                .domain(isLocal ? null : ".hertz-tuning.com")  // isLocal일 경우 domain 생략
                .httpOnly(true)
                .secure(!isLocal) // isLocal=false면 secure 활성화
                .build();

        response.setHeader("Set-Cookie", responseCookie.toString());

        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.ACCESS_TOKEN_REISSUED, "Access Token이 재발급되었습니다.", accessTokenResponse)
        );
    }

    @PostMapping("/login")
    @Operation(summary = "사용자 Id로 AT를 반환하는 API (테스트용)", description = "회원가입 안된 임의의 사용자의 Id도 사용 가능")
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

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
