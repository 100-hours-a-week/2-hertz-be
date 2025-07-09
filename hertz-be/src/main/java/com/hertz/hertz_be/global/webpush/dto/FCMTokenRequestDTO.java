package com.hertz.hertz_be.global.webpush.dto;

import jakarta.validation.constraints.NotBlank;

public record FCMTokenRequestDTO(
        @NotBlank(message = "토큰은 필수입니다.")
        String token
) {}
