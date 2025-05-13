package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDTO;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authTokenService;

    @Value("${is.local}")
    private boolean isLocal;

    @PostMapping("/v1/auth/token")
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

        String cookieValue = String.format( // Todo: 나중에 util 클래스로 분리
                "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=" + (isLocal ? "None;" : "None; Secure;"),
                newRefreshToken, 1209600
        );
        response.setHeader("Set-Cookie", cookieValue);

        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.ACCESS_TOKEN_REISSUED, "Access Token이 재발급되었습니다.", accessTokenResponse)
        );
    }


    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("refreshToken")) {
                return cookie.getValue();
            }
        }
        return null;
    }
}