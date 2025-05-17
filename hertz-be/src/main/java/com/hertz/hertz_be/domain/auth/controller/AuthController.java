package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDTO;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.service.AuthService;
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
