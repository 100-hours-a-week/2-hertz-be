package com.hertz.hertz_be.domain.interests.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterestsResponseDto {
    private String code;
    private String accessToken;
    private String refreshToken;
    private int refreshSecondsUntilExpiry;
}
