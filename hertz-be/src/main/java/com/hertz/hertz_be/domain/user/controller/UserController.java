package com.hertz.hertz_be.domain.user.controller;

import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.service.UserService;
import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 사용자 생성
     * @param userInfoRequestDto
     * @author daisy.lee
     */
    @PostMapping("/users")
    public ResponseEntity<ResponseDto<UserInfoResponseDto>> createUser(@RequestBody UserInfoRequestDto userInfoRequestDto) {
        UserInfoResponseDto userInfoResponseDto = userService.createUser(userInfoRequestDto);

        return ResponseEntity.ok(
                new ResponseDto<>("PROFILE_SAVED_SUCCESSFULLY", "개인정보가 정상적으로 저장되었습니다.", userInfoResponseDto)
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
                new ResponseDto<>("NICKNAME_CREATED", "닉네임이 성공적으로 생성되었습니다.", data)
        );
    }


}
