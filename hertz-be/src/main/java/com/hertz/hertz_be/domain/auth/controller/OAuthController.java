package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.request.OAuthLoginRequestDto;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResponseDto;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResult;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthSignupResponseDto;
import com.hertz.hertz_be.domain.auth.service.OAuthService;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "OAuth 관련 API")
public class OAuthController {

    private final OAuthService oAuthService;
    private final ChannelService channelService;

    @Value("${is.local}")
    private boolean isLocal;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    @GetMapping("/v1/oauth/{provider}/redirection")
    @Operation(summary = "소셜 로그인 리디렉션 API")
    public ResponseEntity<Void> getOAuthRedirectUrl(@PathVariable String provider) {
        String url = oAuthService.getRedirectUrl(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url)
                .build();
    }

    @PostMapping("/v1/oauth/{provider}")
    @Operation(summary = "소셜 로그인 API")
    public ResponseEntity<?> oauthLogin(
            @PathVariable String provider,
            @RequestBody OAuthLoginRequestDto request,
            HttpServletResponse response
    ) {
        OAuthLoginResult result = oAuthService.oauthLogin(provider, request);

        if (result.registered()) {
            String newRefreshToken = result.refreshToken();

            AuthUtil.setRefreshTokenCookie(response, newRefreshToken, maxAgeSeconds, isLocal);

            OAuthLoginResponseDto dto = new OAuthLoginResponseDto(result.userId(), result.accessToken());

            boolean hasSelectedInterests = channelService.hasSelectedInterests(channelService.getUserById(result.userId()));
            if (!hasSelectedInterests) {
                return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_INTERESTS_NOT_SELECTED, "사용자가 아직 취향 선택을 완료하지 않았습니다.", dto));
            }
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_ALREADY_REGISTERED, "로그인에 성공했습니다.", dto));

        } else {
            OAuthSignupResponseDto dto = new OAuthSignupResponseDto(result.providerId());
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_NOT_REGISTERED, "신규 회원입니다.", dto));
        }
    }
}
