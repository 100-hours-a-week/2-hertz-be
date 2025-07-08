package com.hertz.hertz_be.domain.user.controller.v2;

import com.hertz.hertz_be.domain.user.dto.response.v2.UserProfileDTO;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.service.v2.UserService;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController("userControllerV2")
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "사용자 관련 V2 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 정보 조회 (마이페이지, 상대방 상세 조회 페이지)
     * @author daisy.lee
     */
    @GetMapping("/v2/users/{userId}")
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
