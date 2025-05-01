package com.hertz.hertz_be.domain.user.service;
//
//import com.hertz.hertz_be.domain.user.dto.OauthTokenInfo;
//import com.hertz.hertz_be.domain.user.dto.request.UserInfoRequestDto;
//import com.hertz.hertz_be.domain.user.dto.response.UserInfoResponseDto;
//import com.hertz.hertz_be.domain.user.entity.User;
//import com.hertz.hertz_be.domain.user.entity.UserOauth;
//import com.hertz.hertz_be.domain.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//    private final UserRepository userRepository;
//    private final RedisTemplate<String, Object> redisTemplate;
//    private UserInfoResponseDto userInfoResponseDto;
//
//    public UserInfoResponseDto createUser(UserInfoRequestDto userInfoRequestDto) {
//        // 랜덤 닉네임 반환 (무한루프)
//
//        String redisKey = "kakao:oauth:providerId:" + userInfoRequestDto.getProviderId();
//
//        OauthTokenInfo tokenInfo = (OauthTokenInfo) redisTemplate.opsForValue().get(redisKey);
//        if (tokenInfo == null) {
//            // throw new InvalidOauthStateException("OAuth 인증 정보가 만료되었거나 존재하지 않습니다.");
//        }
//
//        User user = User.builder()
//                .ageGroup(userInfoRequestDto.getAgeGroup())
//                .profileImageUrl(userInfoRequestDto.getProfileImage())
//                .nickname(userInfoRequestDto.getNickname())
//                .gender(userInfoRequestDto.getGender())
//                .oneLineIntroduction(userInfoRequestDto.getOneLineIntroduction())
//                .build();
//
//        UserOauth userOauth = UserOauth.builder()
//                .provider(userInfoRequestDto.getProvider())
//                .providerId(userInfoRequestDto.getProviderId())
//                .refreshToken(tokenInfo.getRefreshToken())
//                .refreshTokenExpiresAt(tokenInfo.getRefreshTokenExpiresAt())
//                .user(user)
//                .build();
//
//        user.setUserOauth(userOauth);
//
//        User savedUser = userRepository.save(user);
//
//        return UserInfoResponseDto.builder()
//                .userId(savedUser.getId())
//                .build();
//
//
//    }
//}
