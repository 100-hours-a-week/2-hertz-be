package com.hertz.hertz_be.domain.interests.controller;

import com.hertz.hertz_be.domain.interests.dto.request.UserInterestsRequestDto;
import com.hertz.hertz_be.domain.interests.dto.response.UserInterestsResponseDto;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InterestsController {

    private final InterestsService interestsService;

    public InterestsController(InterestsService interestsService) {
        this.interestsService = interestsService;
    }

    /**
     * 취향 등록
     * @param userInterestsRequestDto
     * @author daisy.lee
     */
    @PostMapping("/users/interests")
    public ResponseEntity<ResponseDto<UserInterestsResponseDto>> createUser(@RequestBody UserInterestsRequestDto userInterestsRequestDto,
                                                                            @AuthenticationPrincipal Long userId) throws Exception {
        interestsService.saveUserInterests(userInterestsRequestDto, userId);

        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.INTERESTS_SAVED_SUCCESSFULLY, "개인정보가 정상적으로 저장되었습니다.", null)
        );
    }
}
