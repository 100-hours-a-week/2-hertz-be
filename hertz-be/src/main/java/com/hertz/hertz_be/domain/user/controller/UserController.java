package com.hertz.hertz_be.domain.user.controller;

import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.service.UserService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 사용자 생성 (개인정보 등록)
     * @param userInfoRequestDto
     * @author daisy.lee
     */
    @PostMapping("/users")
    public ResponseEntity<ResponseDto<Map<String, Object>>> createUser(@RequestBody UserInfoRequestDto userInfoRequestDto,
                                                                       HttpServletResponse response) {
        UserInfoResponseDto userInfoResponseDto = userService.createUser(userInfoRequestDto);

        Cookie refreshTokenCookie = new Cookie("refreshToken", userInfoResponseDto.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // 배포환경에서만 true
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(userInfoResponseDto.getRefreshSecondsUntilExpiry()); // 만료일자까지 남은 시간

        response.addCookie(refreshTokenCookie);

        // 응답 바디에 포함할 정보만 따로 구성
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfoResponseDto.getUserId());
        data.put("accessToken", userInfoResponseDto.getAccessToken());

        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.PROFILE_SAVED_SUCCESSFULLY,
                        "개인정보가 정상적으로 저장되었습니다.",
                        data)
        );

    }

    /**
     * 랜덤 닉네임 반환
     * @author daisy.lee
     */
    @GetMapping("/nickname")
    public ResponseEntity<ResponseDto<Map<String, String>>> generateNickname() {
        String nickname = userService.fetchRandomNickname();
        Map<String, String> data = Map.of("nickname", nickname);
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.NICKNAME_CREATED, "닉네임이 성공적으로 생성되었습니다.", data)
        );
    }

}

