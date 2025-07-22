package com.hertz.hertz_be.domain.user.controller.v3;

import com.hertz.hertz_be.domain.user.dto.request.v3.OneLineIntroductionRequestDto;
import com.hertz.hertz_be.domain.user.dto.request.v3.RejectCategoryChangeRequestDto;
import com.hertz.hertz_be.domain.user.dto.request.v3.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v3.UserProfileDTO;
import com.hertz.hertz_be.domain.user.dto.response.v3.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.service.v3.UserService;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController("userControllerV3")
@RequestMapping("/api/v3")
@RequiredArgsConstructor
@Tag(name = "사용자 관련 V3 API")
public class UserController {

    @Value("${is.local}")
    private boolean isLocal;

    private final UserService userService;

    @PostMapping("/users")
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

    @PatchMapping("/users/category")
    @Operation(summary = "시그널 받고 싶지 않은 카테고리 수정 API")
    public ResponseEntity<ResponseDto<Void>> changeRejectCategory(
            @RequestBody @Valid RejectCategoryChangeRequestDto requestDto,
            @AuthenticationPrincipal Long userId) {

        userService.changeRejectCategory(userId, requestDto);

        return  ResponseEntity.ok(
                new ResponseDto<>(
                        UserResponseCode.CATEGORY_UPDATED_SUCCESSFULLY.getCode(),
                        UserResponseCode.CATEGORY_UPDATED_SUCCESSFULLY.getMessage(),
                        null
                )
        );
    }

    @PatchMapping("/users/{userId}")
    @Operation(summary = "마이페이지 한 줄 소개 수정 API")
    public ResponseEntity<ResponseDto<Void>> updateOneLineIntroduction(@RequestBody @Valid OneLineIntroductionRequestDto requestDto,
                                                                       @PathVariable Long userId,
                                                                       @AuthenticationPrincipal Long requesterId) {

        userService.updateOneLineIntroduction(userId, requesterId, requestDto);

        return ResponseEntity.ok(
                new ResponseDto<>(
                    UserResponseCode.PROFILE_UPDATED_SUCCESSFULLY.getCode(),
                    UserResponseCode.PROFILE_UPDATED_SUCCESSFULLY.getMessage(),
                    null
                )
        );
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "사용자 정보 조회 API")
    public ResponseEntity<ResponseDto<UserProfileDTO>> getUserProfile(@PathVariable Long userId,
                                                                      @AuthenticationPrincipal Long id) {
        UserProfileDTO response = userService.getUserProfile(userId, id);

        if("ME".equals(response.getRelationType())) {
            return ResponseEntity.ok(
                    new ResponseDto<>(
                            UserResponseCode.USER_INFO_FETCH_SUCCESS.getCode(),
                            UserResponseCode.USER_INFO_FETCH_SUCCESS.getMessage(),
                            response)
            );
        } else {
            return ResponseEntity.ok(
                    new ResponseDto<>(
                            UserResponseCode.OTHER_USER_INFO_FETCH_SUCCESS.getCode(),
                            UserResponseCode.OTHER_USER_INFO_FETCH_SUCCESS.getMessage(),
                            response)
            );
        }
    }

}
