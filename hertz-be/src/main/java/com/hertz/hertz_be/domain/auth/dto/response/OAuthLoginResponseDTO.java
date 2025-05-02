package com.hertz.hertz_be.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginResponseDTO {
    private Long userId;
    private String accessToken;

}
