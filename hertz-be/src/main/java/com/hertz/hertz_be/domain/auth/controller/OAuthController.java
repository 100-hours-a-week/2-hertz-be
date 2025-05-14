package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.request.OAuthLoginRequestDTO;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResponseDTO;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResult;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthSignupResponseDTO;
import com.hertz.hertz_be.domain.auth.service.OAuthService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    @Value("${is.local}")
    private boolean isLocal;

    @GetMapping("/v1/oauth/{provider}/redirection")
    public ResponseEntity<Void> getOAuthRedirectUrl(@PathVariable String provider) {
        String url = oAuthService.getRedirectUrl(provider);
        return ResponseEntity.status(302)
                .header("Location", url)
                .build();
    }

    @PostMapping("/v1/oauth/{provider}")
    public ResponseEntity<?> oauthLogin(
            @PathVariable String provider,
            @RequestBody OAuthLoginRequestDTO request,
            HttpServletResponse response
    ) {
        OAuthLoginResult result = oAuthService.oauthLogin(provider, request);

        if (result.isRegistered()) {
            String newRefreshToken = result.getRefreshToken();

            //ResponseCookie 설정 (환경에 따라 분기)
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie
                    .from("refreshToken", newRefreshToken)
                    .maxAge(1209600)
                    .path("/")
                    .httpOnly(true)
                    .domain("dev.hertz-tuning.com")
                    .sameSite("None");

            if (!isLocal) {
                cookieBuilder
                        .secure(true);
            }

            ResponseCookie responseCookie = cookieBuilder.build();
            response.setHeader("Set-Cookie", responseCookie.toString());

            OAuthLoginResponseDTO dto = new OAuthLoginResponseDTO(result.getUserId(), result.getAccessToken());
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_ALREADY_REGISTERED, "로그인에 성공했습니다.", dto));

        } else {
            OAuthSignupResponseDTO dto = new OAuthSignupResponseDTO(result.getProviderId());
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_NOT_REGISTERED, "신규 회원입니다.", dto));
        }
    }
}
