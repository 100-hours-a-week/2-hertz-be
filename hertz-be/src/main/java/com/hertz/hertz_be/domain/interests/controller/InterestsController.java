package com.hertz.hertz_be.domain.interests.controller;

import com.hertz.hertz_be.domain.interests.dto.request.UserInterestsRequestDto;
import com.hertz.hertz_be.domain.interests.dto.response.UserInterestsResponseDto;
import com.hertz.hertz_be.domain.interests.responsecode.InterestsResponseCode;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "JWT")
@Tag(name = "취향 관련 API")
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
    @Operation(summary = "취향 등록 API")
    public ResponseEntity<ResponseDto<UserInterestsResponseDto>> createUser(@RequestBody UserInterestsRequestDto userInterestsRequestDto,
                                                                            @AuthenticationPrincipal Long userId) throws Exception {
        interestsService.saveUserInterests(userInterestsRequestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(InterestsResponseCode.INTERESTS_SAVED_SUCCESSFULLY.getCode(), InterestsResponseCode.INTERESTS_SAVED_SUCCESSFULLY.getMessage(), null));
    }
}
