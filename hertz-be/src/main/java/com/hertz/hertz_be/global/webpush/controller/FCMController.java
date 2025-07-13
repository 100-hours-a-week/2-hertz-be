package com.hertz.hertz_be.global.webpush.controller;

import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.webpush.dto.FCMTokenRequestDTO;
import com.hertz.hertz_be.global.webpush.responsecode.FCMResponseCode;
import com.hertz.hertz_be.global.webpush.service.FCMService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/push")
public class FCMController {

    private final FCMService fcmService;

    @PostMapping("/token")
    public ResponseEntity<ResponseDto<Void>> saveFCMToken(@AuthenticationPrincipal Long userId,
                                                                         @RequestBody @Valid FCMTokenRequestDTO request) {
        fcmService.saveToken(userId, request.token());
        return ResponseEntity
                .status(FCMResponseCode.FCM_TOKEN_SAVED_SUCCESS.getHttpStatus())
                .body(new ResponseDto<>(
                        FCMResponseCode.FCM_TOKEN_SAVED_SUCCESS.getCode(),
                        FCMResponseCode.FCM_TOKEN_SAVED_SUCCESS.getMessage(),
                        null
                ));
    }
}