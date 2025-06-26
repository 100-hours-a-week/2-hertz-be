package com.hertz.hertz_be.domain.user.service.v3;

import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.user.dto.request.v3.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v3.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import com.hertz.hertz_be.domain.user.repository.UserOauthRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserOauthRepository userOauthRepository;

    @Mock
    private OAuthRedisRepository oauthRedisRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Value("${invitation.code.kakaotech}")
    private int kakaotechInvitationCode;

    @Value("${invitation.code.outside}")
    private int outsideInvitationCode;

    private final String providerId = "test-provider-id";
    private final String provider = "kakao";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "kakaotechInvitationCode", kakaotechInvitationCode);
        ReflectionTestUtils.setField(userService, "outsideInvitationCode", outsideInvitationCode);
        ReflectionTestUtils.setField(userService, "maxAgeSeconds", 1209600L);
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void createUser_success() {
        // given
        UserInfoRequestDto requestDto = UserInfoRequestDto.builder()
                .providerId(providerId)
                .provider(provider)
                .nickname("nickname")
                .ageGroup(AgeGroup.AGE_20S)
                .gender(Gender.MALE)
                .oneLineIntroduction("hello")
                .coupleAllowed(true)
                .friendAllowed(false)
                .invitationCode(kakaotechInvitationCode)
                .build();

        String redisValue = "refresh-token,2999-12-31T00:00:00";
        when(oauthRedisRepository.get(providerId)).thenReturn(redisValue);
        when(userOauthRepository.existsByProviderIdAndProvider(providerId, provider)).thenReturn(false);

        User mockUser = User.builder().id(1L).build();
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        when(jwtTokenProvider.createRefreshToken(mockUser.getId())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.createAccessToken(mockUser.getId())).thenReturn("new-access-token");

        // when
        UserInfoResponseDto result = userService.createUser(requestDto);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());

        verify(oauthRedisRepository).get(providerId);
        verify(userOauthRepository).existsByProviderIdAndProvider(providerId, provider);
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).saveRefreshToken(eq(1L), eq("new-refresh-token"), eq(1209600L));
    }

    @Test
    @DisplayName("회원가입 - OAuth Redis 값 없음")
    void createUser_shouldThrowBusinessException_whenRedisValueIsNull() {
        // given
        UserInfoRequestDto requestDto = UserInfoRequestDto.builder()
                .providerId(providerId)
                .provider(provider)
                .invitationCode(kakaotechInvitationCode)
                .build();

        when(oauthRedisRepository.get(providerId)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(requestDto);
        });

        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("회원가입 - 중복 유저")
    void createUser_shouldThrowBusinessException_whenDuplicateUser() {
        // given
        UserInfoRequestDto requestDto = UserInfoRequestDto.builder()
                .providerId(providerId)
                .provider(provider)
                .invitationCode(kakaotechInvitationCode)
                .build();

        when(oauthRedisRepository.get(providerId)).thenReturn("token,2999-12-31T00:00:00");
        when(userOauthRepository.existsByProviderIdAndProvider(providerId, provider)).thenReturn(true);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(requestDto);
        });

        assertEquals(UserResponseCode.DUPLICATE_USER.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("회원가입 - 잘못된 초대코드")
    void createUser_shouldThrowBusinessException_whenWrongInvitationCode() {
        // given
        UserInfoRequestDto requestDto = UserInfoRequestDto.builder()
                .providerId(providerId)
                .provider(provider)
                .invitationCode(9999)
                .build();

        when(oauthRedisRepository.get(providerId)).thenReturn("token,2999-12-31T00:00:00");
        when(userOauthRepository.existsByProviderIdAndProvider(providerId, provider)).thenReturn(false);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(requestDto);
        });

        assertEquals(UserResponseCode.WRONG_INVITATION_CODE.getCode(), exception.getCode());
    }
}
