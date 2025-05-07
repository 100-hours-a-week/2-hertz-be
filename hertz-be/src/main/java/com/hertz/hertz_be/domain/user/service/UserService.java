package com.hertz.hertz_be.domain.user.service;

import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OAuthRedisRepository oauthRedisRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final long TIMEOUT_NANOS = 5_000_000_000L; // // 5초 = 5_000_000_000 나노초

    @Value("${external.api.nickname-url}")
    private String NICKNAME_API_URL;


    public UserInfoResponseDto createUser(UserInfoRequestDto userInfoRequestDto) {
        String redisValue = oauthRedisRepository.get(userInfoRequestDto.getProviderId());
        if (redisValue == null) {
            throw new UserException(ResponseCode.REFRESH_TOKEN_INVALID, "Refresh Token이 올바르지 않습니다.");
        }

        String refreshTokenValue = redisValue.split(",")[0];
        LocalDateTime refreshTokenExpiredAt = LocalDateTime.parse(redisValue.split(",")[1]);

        // 현재 시간과 refreshToken의 만료 일시까지를 계산 (cookie 만료 시간 설정 시 만료일자를 정할 수 없기 때문)
        long secondsUntilExpiry = Duration.between(LocalDateTime.now(), refreshTokenExpiredAt).getSeconds();
        int maxAge = (int) Math.max(0, secondsUntilExpiry);

        // Todo. FE 개발용 테스트 로직 (추후 삭제 필요)
        if(userInfoRequestDto.isTest()) {
            Long fakeUserId = -1L;

            return UserInfoResponseDto.builder()
                    .userId(fakeUserId)
                    .accessToken(jwtTokenProvider.createAccessToken(fakeUserId))
                    .refreshToken(refreshTokenValue)
                    .refreshSecondsUntilExpiry(maxAge)
                    .build();
        }

        User user = User.builder()
                .ageGroup(userInfoRequestDto.getAgeGroup())
                .profileImageUrl(userInfoRequestDto.getProfileImage())
                .nickname(userInfoRequestDto.getNickname())
                .email(userInfoRequestDto.getProviderId() + "@kakaotech.com") //
                .gender(userInfoRequestDto.getGender())
                .oneLineIntroduction(userInfoRequestDto.getOneLineIntroduction())
                .build();

        UserOauth userOauth = UserOauth.builder()
                .provider(userInfoRequestDto.getProvider())
                .providerId(userInfoRequestDto.getProviderId())
                .refreshToken(refreshTokenValue)
                .refreshTokenExpiresAt(refreshTokenExpiredAt)
                .user(user)
                .build();

        user.setUserOauth(userOauth);

        User savedUser = userRepository.save(user);

        return UserInfoResponseDto.builder()
                .userId(savedUser.getId())
                .accessToken(jwtTokenProvider.createAccessToken(savedUser.getId()))
                .refreshToken(refreshTokenValue)
                .refreshSecondsUntilExpiry(maxAge)
                .build();

    }

    public String fetchRandomNickname() {
        final long startTime = System.nanoTime();

        while (true) {
            if (System.nanoTime() - startTime > TIMEOUT_NANOS) {
                throw new UserException(ResponseCode.NICKNAME_GENERATION_TIMEOUT, "5초 내에 중복되지 않은 닉네임을 찾지 못했습니다.");
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


