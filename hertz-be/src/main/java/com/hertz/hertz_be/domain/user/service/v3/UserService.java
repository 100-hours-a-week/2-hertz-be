package com.hertz.hertz_be.domain.user.service.v3;

import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.user.dto.request.v3.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v3.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.repository.UserOauthRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service("userServiceV3")
@RequiredArgsConstructor
public class UserService {

    private final UserOauthRepository userOauthRepository;
    private final OAuthRedisRepository oauthRedisRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    public UserInfoResponseDto createUser(UserInfoRequestDto userInfoRequestDto) {
        String redisValue = oauthRedisRepository.get(userInfoRequestDto.getProviderId());
        if (redisValue == null) {
            throw new BusinessException(
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
        }

        if (userOauthRepository.existsByProviderIdAndProvider(userInfoRequestDto.getProviderId(), userInfoRequestDto.getProvider())) {
            throw new BusinessException(
                    UserResponseCode.DUPLICATE_USER.getCode(),
                    UserResponseCode.DUPLICATE_USER.getHttpStatus(),
                    UserResponseCode.DUPLICATE_USER.getMessage());
        }

        int invitationCode = userInfoRequestDto.getInvitationCode();
        if (invitationCode != 2502 && invitationCode != 6739) {
            throw new BusinessException(
                    UserResponseCode.WRONG_INVITATION_CODE.getCode(),
                    UserResponseCode.WRONG_INVITATION_CODE.getHttpStatus(),
                    UserResponseCode.WRONG_INVITATION_CODE.getMessage());
        }

        String refreshTokenValue = redisValue.split(",")[0];
        LocalDateTime refreshTokenExpiredAt = LocalDateTime.parse(redisValue.split(",")[1]);

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

        String userDomain;
        if (invitationCode == 2502) {
            userDomain = "@kakaotech.com";
        } else {
            userDomain = "@outside.com";
        }

        User user = User.builder()
                .ageGroup(userInfoRequestDto.getAgeGroup())
                .profileImageUrl(userInfoRequestDto.getProfileImage())
                .nickname(userInfoRequestDto.getNickname())
                .email(userInfoRequestDto.getProviderId() + userDomain) //
                .gender(userInfoRequestDto.getGender())
                .oneLineIntroduction(userInfoRequestDto.getOneLineIntroduction())
                .isCoupleAllowed(userInfoRequestDto.isCoupleAllowed())
                .isFriendAllowed(userInfoRequestDto.isFriendAllowed())
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

        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);

        return UserInfoResponseDto.builder()
                .userId(savedUser.getId())
                .accessToken(jwtTokenProvider.createAccessToken(savedUser.getId()))
                .refreshToken(refreshToken)
                .refreshSecondsUntilExpiry(maxAge)
                .build();

    }
}
