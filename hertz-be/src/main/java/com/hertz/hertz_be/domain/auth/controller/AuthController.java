package com.hertz.hertz_be.domain.auth.controller;

import static com.hertz.hertz_be.global.util.AuthUtil.extractRefreshTokenFromCookie;
import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.auth.dto.request.TestLoginRequestDto;
import com.hertz.hertz_be.domain.user.service.UserService;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final UserService userService;
    private final AuthService authService;

    @Value("${is.local}")
    private boolean isLocal;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    @PostMapping("/v1/auth/token")
    @Operation(summary = "Access Token 재발급 API")
    public ResponseEntity<ResponseDto<ReissueAccessTokenResponseDto>> reissueAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new RefreshTokenInvalidException();
        }

        Map.Entry<ReissueAccessTokenResponseDto, String> result = authTokenService.reissueAccessToken(refreshToken);
        ReissueAccessTokenResponseDto accessTokenResponse = result.getKey();
        String newRefreshToken = result.getValue();

        AuthUtil.setRefreshTokenCookie(response, newRefreshToken, maxAgeSeconds, isLocal);

        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.ACCESS_TOKEN_REISSUED, "Access Token이 재발급되었습니다.", accessTokenResponse)
        );
    }

    @PostMapping("/login")
    @Operation(summary = "사용자 Id로 AT와 RT를 반환하는 API(테스트용)", description = "회원가입 안된 임의의 사용자의 Id도 사용 가능")
    public ResponseEntity<?> login(@RequestBody TestLoginRequestDto request,
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

    @DeleteMapping("/{userId}")
    @Operation(summary = "userId로 특정 사용자 삭제 API (테스트용)",  description = "해당 사용자가 참여중인 모든 채팅방도 삭제 주의")
    public ResponseEntity<ResponseDto<Void>> deleteUserById(@PathVariable Long userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.USER_DELETE_SUCCESS, "사용자가 정상적으로 삭제되었습니다.", null)
        );
    }

    @DeleteMapping("/users")
    @Operation(summary = "DB에 있는 모든 사용자 정보 및 과련 모든 데이터를 삭제하는 API (테스트용)")
    public ResponseEntity<ResponseDto<Void>> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.USER_DELETE_SUCCESS, "모든 사용자와 사용자 관련 데이터 모두 정상적으로 삭제되었습니다.", null)
        );
    }

    @DeleteMapping("/v2/auth/logout")
    @Operation(summary = "로그아웃 API")
    public ResponseEntity<ResponseDto<Void>> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.LOGOUT_SUCCESS, "정상적으로 로그아웃되었습니다.", null)
        );
    }
}
