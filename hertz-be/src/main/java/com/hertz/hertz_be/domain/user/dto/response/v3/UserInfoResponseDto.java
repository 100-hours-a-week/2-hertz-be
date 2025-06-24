package com.hertz.hertz_be.domain.user.dto.response.v3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponseDto {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private int refreshSecondsUntilExpiry;
}
