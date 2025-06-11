package com.hertz.hertz_be.domain.user.controller;

import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.dto.response.UserProfileDTO;
import com.hertz.hertz_be.domain.user.service.UserService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "사용자 관련 API")
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
            @RequestBody UserInfoRequestDto userInfoRequestDto,
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
                        ResponseCode.PROFILE_SAVED_SUCCESSFULLY,
                        "개인정보가 정상적으로 저장되었습니다.",
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
                new ResponseDto<>(ResponseCode.NICKNAME_CREATED, "닉네임이 성공적으로 생성되었습니다.", data)
        );
    }


    /**
     * 사용자 정보 조회 (마이페이지, 상대방 상세 조회 페이지)
     * @author daisy.lee
     */
    @GetMapping("/v2/users/{userId}")
    @Operation(summary = "사용자 정보 조회")
    public ResponseEntity<ResponseDto<UserProfileDTO>> getUserProfile(@PathVariable Long userId,
                                                                      @AuthenticationPrincipal Long id) {
        UserProfileDTO response = userService.getUserProfile(userId, id);

        if("ME".equals(response.getRelationType())) {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.USER_INFO_FETCH_SUCCESS, "사용자의 정보가 정상적으로 조회되었습니다.", response)
            );
        } else {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.OTHER_USER_INFO_FETCH_SUCCESS, "상대방의 정보가 정상적으로 조회되었습니다.", response)
            );
        }
    }
}
