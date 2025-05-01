package com.hertz.hertz_be.domain.user.controller;

//import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
//import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
//import com.hertz.hertz_be.domain.user.service.UserService;
//import com.hertz.hertz_be.global.common.dto.ApiResponseDto;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1")
//public class UserController {
//    private final UserService userService;
//
//    public UserController(UserService userService) {
//        this.userService = userService;
//    }
//
//    @PostMapping("/users")
//    public ResponseEntity<ResponseDto<UserInfoResponseDto>> createUser(@RequestBody UserInfoRequestDto userInfoRequestDto) {
//        UserInfoResponseDto userInfoResponseDto = userService.createUser(userInfoRequestDto);
//
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponseDto.success(
//                        "PROFILE_SAVED_SUCCESSFULLY",
//                        "개인정보가 정상적으로 저장되었습니다.",
//                        userInfoResponseDto
//                ));
//    }
//}
