package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.dto.request.OAuthLoginRequestDTO;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResponseDTO;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResult;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthSignupResponseDTO;
import com.hertz.hertz_be.domain.auth.service.OAuthService;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final ChannelService channelService;

    @Value("${is.local}")
    private boolean isLocal;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    @GetMapping("/v1/oauth/{provider}/redirection")
    public ResponseEntity<Void> getOAuthRedirectUrl(@PathVariable String provider) {
        String url = oAuthService.getRedirectUrl(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
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
            ResponseCookie responseCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .maxAge(maxAgeSeconds)
                    .path("/")
                    .sameSite("None")
                    .domain(isLocal ? null : ".hertz-tuning.com")  // isLocal일 경우 domain 생략
                    .httpOnly(true)
                    .secure(!isLocal)                               // isLocal=false면 secure 활성화
                    .build();

            response.setHeader("Set-Cookie", responseCookie.toString());

            OAuthLoginResponseDTO dto = new OAuthLoginResponseDTO(result.getUserId(), result.getAccessToken());

            boolean hasSelectedInterests = channelService.hasSelectedInterests(channelService.getUserById(result.getUserId()));
            if (!hasSelectedInterests) {
                return ResponseEntity
                        .status(HttpStatus.OK) // Todo. 300번대로 리팩토링 필요
                        .body(new ResponseDto<>(ResponseCode.USER_INTERESTS_NOT_SELECTED, "사용자가 아직 취향 선택을 완료하지 않았습니다.", dto));
            }
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_ALREADY_REGISTERED, "로그인에 성공했습니다.", dto));

        } else {
            OAuthSignupResponseDTO dto = new OAuthSignupResponseDTO(result.getProviderId());
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.USER_NOT_REGISTERED, "신규 회원입니다.", dto));
        }
    }
}
