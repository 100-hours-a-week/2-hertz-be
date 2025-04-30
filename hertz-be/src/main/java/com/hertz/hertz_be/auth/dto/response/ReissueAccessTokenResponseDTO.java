package com.hertz.hertz_be.auth.dto.response;

public class ReissueAccessTokenResponseDTO {
    private final String accessToken;

    public ReissueAccessTokenResponseDTO(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}

