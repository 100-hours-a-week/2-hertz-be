package com.hertz.hertz_be.domain.auth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    public Map<String, Object> getUserInfoAndTokens(String code) {
        // 카카오 Access Token & Refresh Token 발급
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        LinkedMultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "authorization_code");
        tokenRequest.add("client_id", clientId);
        tokenRequest.add("redirect_uri", redirectUri);
        tokenRequest.add("code", code);

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<LinkedMultiValueMap<String, String>> tokenEntity = new HttpEntity<>(tokenRequest, tokenHeaders);

        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenEntity, String.class);

        Map<String, Object> tokenBody = parseJson(tokenResponse.getBody());
        String accessToken = tokenBody.get("access_token").toString();
        String refreshToken = tokenBody.get("refresh_token").toString();
        String refreshTokenExpiresIn = tokenBody.get("refresh_token_expires_in").toString();

        // 발급받은 카카오 토큰으로 사용자 정보 요청
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

        ResponseEntity<String> userResponse = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                userEntity,
                String.class
        );

        Map<String, Object> userData = parseJson(userResponse.getBody());

        // 필요한 정보 추출
        Map<String, Object> result = new HashMap<>();
        result.put("id", userData.get("id")); // 카카오 ID(고유한값)
        result.put("refresh_token", refreshToken);
        result.put("refresh_token_expires_in", refreshTokenExpiresIn);

        return result;
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}