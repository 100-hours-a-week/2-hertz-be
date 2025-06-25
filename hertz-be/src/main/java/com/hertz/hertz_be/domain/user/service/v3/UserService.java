package com.hertz.hertz_be.domain.user.service.v3;

import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.user.dto.request.v3.RejectCategoryChangeRequestDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service("userServiceV3")
@RequiredArgsConstructor
public class UserService {

    private static final String KAKAOTECH_DOMAIN = "@kakaotech.com";
    private static final String OUTSIDE_DOMAIN = "@outside.com";

    private final UserOauthRepository userOauthRepository;
    private final OAuthRedisRepository oauthRedisRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${invitation.code.kakaotech}")
    private int kakaotechInvitationCode;

    @Value("${invitation.code.outside}")
    private int outsideInvitationCode;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    public UserInfoResponseDto createUser(UserInfoRequestDto userInfoRequestDto) {
        String redisValue = oauthRedisRepository.get(userInfoRequestDto.getProviderId());
        validateRedisValue(redisValue);
        validateDuplicateUser(userInfoRequestDto);
        validateInvitationCode(userInfoRequestDto.getInvitationCode());

        String[] parts = redisValue.split(",");
        String redisRefreshToken = parts[0];
        LocalDateTime redisRefreshTokenExpiredAt = LocalDateTime.parse(parts[1]);

        long secondsUntilExpiry = Duration.between(LocalDateTime.now(), redisRefreshTokenExpiredAt).getSeconds();
        int maxAge = (int) Math.max(0, secondsUntilExpiry);

        // TODO: FE 개발용 테스트 로직 -> 추후 제거
        if (userInfoRequestDto.isTest()) {
            return buildUserInfoResponse(-1L, redisRefreshToken, maxAge);
        }

        String userDomain = resolveUserDomain(userInfoRequestDto.getInvitationCode());

        User user = buildUser(userInfoRequestDto, userDomain);
        UserOauth userOauth = buildUserOauth(userInfoRequestDto, redisRefreshToken, redisRefreshTokenExpiredAt, user);
        user.setUserOauth(userOauth);

        User savedUser = userRepository.save(user);

        String newRefreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());
        refreshTokenRepository.saveRefreshToken(savedUser.getId(), newRefreshToken, maxAgeSeconds);

        return buildUserInfoResponse(savedUser.getId(), newRefreshToken, maxAge);
    }

    private void validateRedisValue(String redisValue) {
        if (redisValue == null) {
            throw new BusinessException(
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
        }
    }

    private void validateDuplicateUser(UserInfoRequestDto dto) {
        if (userOauthRepository.existsByProviderIdAndProvider(dto.getProviderId(), dto.getProvider())) {
            throw new BusinessException(
                    UserResponseCode.DUPLICATE_USER.getCode(),
                    UserResponseCode.DUPLICATE_USER.getHttpStatus(),
                    UserResponseCode.DUPLICATE_USER.getMessage());
        }
    }

    private void validateInvitationCode(int code) {
        if (code != kakaotechInvitationCode && code != outsideInvitationCode) {
            throw new BusinessException(
                    UserResponseCode.WRONG_INVITATION_CODE.getCode(),
                    UserResponseCode.WRONG_INVITATION_CODE.getHttpStatus(),
                    UserResponseCode.WRONG_INVITATION_CODE.getMessage());
        }
    }

    private String resolveUserDomain(int invitationCode) {
        return invitationCode == kakaotechInvitationCode ? KAKAOTECH_DOMAIN : OUTSIDE_DOMAIN;
    }

    private User buildUser(UserInfoRequestDto dto, String userDomain) {
        return User.builder()
                .ageGroup(dto.getAgeGroup())
                .profileImageUrl(dto.getProfileImage())
                .nickname(dto.getNickname())
                .email(dto.getProviderId() + userDomain)
                .gender(dto.getGender())
                .oneLineIntroduction(dto.getOneLineIntroduction())
                .isCoupleAllowed(dto.isCoupleAllowed())
                .isFriendAllowed(dto.isFriendAllowed())
                .build();
    }

    private UserOauth buildUserOauth(UserInfoRequestDto dto, String refreshToken, LocalDateTime expiresAt, User user) {
        return UserOauth.builder()
                .provider(dto.getProvider())
                .providerId(dto.getProviderId())
                .refreshToken(refreshToken)
                .refreshTokenExpiresAt(expiresAt)
                .user(user)
                .build();
    }

    private UserInfoResponseDto buildUserInfoResponse(Long userId, String refreshToken, int secondsUntilExpiry) {
        return UserInfoResponseDto.builder()
                .userId(userId)
                .accessToken(jwtTokenProvider.createAccessToken(userId))
                .refreshToken(refreshToken)
                .refreshSecondsUntilExpiry(secondsUntilExpiry)
                .build();
    }

    @Transactional
    public void changeRejectCategory(Long userId, RejectCategoryChangeRequestDto requestDto) {
        User user = getUserWithSentSignalRoomsOrThrow(userId);
        user.changeRejectCategory(requestDto.getCategory(), requestDto.isFlag());
    }

    private User getUserWithSentSignalRoomsOrThrow(Long userId) {
        return userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()));
    }
}
