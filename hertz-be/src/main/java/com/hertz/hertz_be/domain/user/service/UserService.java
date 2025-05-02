package com.hertz.hertz_be.domain.user.service;

import com.hertz.hertz_be.domain.user.dto.OauthTokenInfo;
import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final long TIMEOUT_NANOS = 5_000_000_000L; // // 5초 = 5_000_000_000 나노초

    @Value("${external.api.nickname-url}")
    private String NICKNAME_API_URL;


    public UserInfoResponseDto createUser(UserInfoRequestDto userInfoRequestDto) {
        // 랜덤 닉네임 반환 (무한루프)

        String redisKey = "kakao:oauth:providerId:" + userInfoRequestDto.getProviderId();

        OauthTokenInfo tokenInfo = (OauthTokenInfo) redisTemplate.opsForValue().get(redisKey);
        if (tokenInfo == null) {
            // throw new InvalidOauthStateException("OAuth 인증 정보가 만료되었거나 존재하지 않습니다.");
        }

        User user = User.builder()
                .ageGroup(userInfoRequestDto.getAgeGroup())
                .profileImageUrl(userInfoRequestDto.getProfileImage())
                .nickname(userInfoRequestDto.getNickname())
                .gender(userInfoRequestDto.getGender())
                .oneLineIntroduction(userInfoRequestDto.getOneLineIntroduction())
                .build();

        UserOauth userOauth = UserOauth.builder()
                .provider(userInfoRequestDto.getProvider())
                .providerId(userInfoRequestDto.getProviderId())
                .refreshToken(tokenInfo.getRefreshToken())
                .refreshTokenExpiresAt(tokenInfo.getRefreshTokenExpiresAt())
                .user(user)
                .build();

        user.setUserOauth(userOauth);

        User savedUser = userRepository.save(user);

        return UserInfoResponseDto.builder()
                .userId(savedUser.getId())
                .build();

    }

    public String fetchRandomNickname() {
        final long startTime = System.nanoTime();

        while (true) {
            if (System.nanoTime() - startTime > TIMEOUT_NANOS) {
                throw new UserException("NICKNAME_GENERATION_TIMEOUT", "5초 내에 중복되지 않은 닉네임을 찾지 못했습니다.");
            }

            String nickname;
            try {
                nickname = callExternalNicknameApi();
            } catch (Exception e) {
                throw new UserException(ResponseCode.NICKNAME_API_FAILED, "닉네임 생성 API 호출 실패");
            }

            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
    }

    private String callExternalNicknameApi() {
        ResponseEntity<String> response = restTemplate.getForEntity(NICKNAME_API_URL, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().trim();
        }
        throw new UserException(ResponseCode.NICKNAME_API_FAILED, "닉네임 생성 API 응답 실패");
    }
}
