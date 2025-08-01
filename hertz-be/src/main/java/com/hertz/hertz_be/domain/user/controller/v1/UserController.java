package com.hertz.hertz_be.domain.user.controller.v1;

import com.hertz.hertz_be.domain.user.dto.request.v1.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v1.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.service.v1.UserService;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController("userControllerV1")
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "사용자 관련 V1 API")
public class UserController {

    @Value("${is.local}")
    private boolean isLocal;

    private final UserService userService;

    /**
     * 사용자 생성 (개인정보 등록)
     * @param userInfoRequestDto
     * @author daisy.lee
     */
    @PostMapping("/v1/users")
    @Operation(summary = "개인정보 등록 API")
    public ResponseEntity<ResponseDto<Map<String, Object>>> createUser(
            @RequestBody @Valid UserInfoRequestDto userInfoRequestDto,
            HttpServletResponse response) {

        UserInfoResponseDto userInfoResponseDto = userService.createUser(userInfoRequestDto);

        AuthUtil.setRefreshTokenCookie(response,
                userInfoResponseDto.getRefreshToken(),
                userInfoResponseDto.getRefreshSecondsUntilExpiry(),
                isLocal
        );

        // ✅ 응답 바디 구성
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfoResponseDto.getUserId());
        data.put("accessToken", userInfoResponseDto.getAccessToken());

        return ResponseEntity.ok(
                new ResponseDto<>(
                        UserResponseCode.PROFILE_SAVED_SUCCESSFULLY.getCode(),
                        UserResponseCode.PROFILE_SAVED_SUCCESSFULLY.getMessage(),
                        data
                )
        );
    }

    /**
     * 랜덤 닉네임 반환
     * @author daisy.lee
     */
    @GetMapping("/v1/nickname")
    @Operation(summary = "랜덤 닉네임 반환 API")
    public ResponseEntity<ResponseDto<Map<String, String>>> generateNickname() {
        String nickname = userService.fetchRandomNickname();
        Map<String, String> data = Map.of("nickname", nickname);
        return ResponseEntity.ok(
                new ResponseDto<>(
                        UserResponseCode.NICKNAME_CREATED.getCode(),
                        UserResponseCode.NICKNAME_CREATED.getMessage(),
                        data)

        );
    }
}
