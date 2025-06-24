package com.hertz.hertz_be.domain.user.controller.v3;

import com.hertz.hertz_be.domain.user.dto.request.v3.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v3.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.service.v3.UserService;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController("userControllerV3")
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "사용자 관련 V3 API")
public class UserController {

    @Value("${is.local}")
    private boolean isLocal;

    private final UserService userService;

    @PostMapping("/v3/users")
    @Operation(summary = "개인정보 등록 API")
    public ResponseEntity<ResponseDto<Map<String, Object>>> createUser(
            @RequestBody UserInfoRequestDto userInfoRequestDto,
            HttpServletResponse response) {

        UserInfoResponseDto userInfoResponseDto = userService.createUser(userInfoRequestDto);

        AuthUtil.setRefreshTokenCookie(response,
                userInfoResponseDto.getRefreshToken(),
                userInfoResponseDto.getRefreshSecondsUntilExpiry(),
                isLocal
        );

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

}
