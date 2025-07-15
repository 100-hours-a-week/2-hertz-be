package com.hertz.hertz_be.global.webpush.controller;

import com.hertz.hertz_be.global.webpush.dto.FCMTokenRequestDTO;
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
    public ResponseEntity<?> saveFCMToken(@AuthenticationPrincipal Long userId,
                                          @RequestBody @Valid FCMTokenRequestDTO request) {
        return fcmService.saveToken(userId, request.token());
    }
}