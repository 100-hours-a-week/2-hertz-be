package com.hertz.hertz_be.domain.user.service.v1;

import com.hertz.hertz_be.domain.alarm.entity.AlarmMatching;
import com.hertz.hertz_be.domain.alarm.entity.AlarmNotification;
import com.hertz.hertz_be.domain.alarm.repository.AlarmMatchingRepository;
import com.hertz.hertz_be.domain.alarm.repository.AlarmNotificationRepository;
import com.hertz.hertz_be.domain.alarm.repository.AlarmRepository;
import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningRepository;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningResultRepository;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.dto.request.v1.UserInfoRequestDto;
import com.hertz.hertz_be.domain.user.dto.response.v1.UserInfoResponseDto;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserOauthRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service("userServiceV1")
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserOauthRepository userOauthRepository;
    private final UserInterestsRepository userInterestsRepository;
    private final OAuthRedisRepository oauthRedisRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final InterestsService interestsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final TuningResultRepository tuningResultRepository;
    private final AlarmNotificationRepository alarmNotificationRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final AlarmMatchingRepository alarmMatchingRepository;
    private final AlarmRepository alarmRepository;
    private final TuningReportRepository tuningReportRepository;
    private final TuningReportUserReactionRepository tuningReportUserReactionRepository;
    private final TuningRepository tuningRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final long TIMEOUT_NANOS = 5_000_000_000L; // // 5초 = 5_000_000_000 나노초

    @Value("${external.api.nickname-url}")
    private String NICKNAME_API_URL;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    @Transactional
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

        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);

        return UserInfoResponseDto.builder()
                .userId(savedUser.getId())
                .accessToken(jwtTokenProvider.createAccessToken(savedUser.getId()))
                .refreshToken(refreshToken)
                .refreshSecondsUntilExpiry(maxAge)
                .build();

    }

    @Transactional
    public String fetchRandomNickname() {
        final long startTime = System.nanoTime();

        while (true) {
            if (System.nanoTime() - startTime > TIMEOUT_NANOS) {
                throw new BusinessException(
                        UserResponseCode.NICKNAME_GENERATION_TIMEOUT.getCode(),
                        UserResponseCode.NICKNAME_GENERATION_TIMEOUT.getHttpStatus(),
                        UserResponseCode.NICKNAME_GENERATION_TIMEOUT.getMessage());
            }

            String nickname;
            try {
                nickname = callExternalNicknameApi();
            } catch (Exception e) {
                throw new BusinessException(
                        UserResponseCode.NICKNAME_API_FAILED.getCode(),
                        UserResponseCode.NICKNAME_API_FAILED.getHttpStatus(),
                        UserResponseCode.NICKNAME_API_FAILED.getMessage());
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
        throw new BusinessException(
                UserResponseCode.NICKNAME_API_FAILED.getCode(),
                UserResponseCode.NICKNAME_API_FAILED.getHttpStatus(),
                UserResponseCode.NICKNAME_API_FAILED.getMessage());
    }

    @Transactional
    public void deleteUserById(Long userId) {
        User user = userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()));

        List<SignalRoom> rooms = signalRoomRepository.findAllBySenderUserIdOrReceiverUserId(userId, userId);

        // 1. 관련된 TuningReport 먼저 조회
        List<TuningReport> tuningReports = tuningReportRepository.findAllBySignalRoomIn(rooms);

        // 2. TuningReportUserReaction → reportId 기준으로 먼저 삭제
        for (TuningReport tuningReport : tuningReports) {
            List<TuningReportUserReaction> reactions = tuningReportUserReactionRepository.findAllByReportId(tuningReport.getId());
            tuningReportUserReactionRepository.deleteAll(reactions);
        }

        // 3. TuningReport 삭제
        tuningReportRepository.deleteAll(tuningReports);

        // 4. SignalMessage 삭제
        for (SignalRoom room : rooms) {
            signalMessageRepository.deleteAllBySignalRoom(room);
        }

        // 5. AlarmNotification 처리
        List<AlarmNotification> notifications = alarmNotificationRepository.findAllByWriter(user);
        notifications.forEach(AlarmNotification::removeWriter);

        // 6. AlarmMatching 처리
        List<AlarmMatching> matchingByUser = alarmMatchingRepository.findAllByPartner(user);
        matchingByUser.forEach(AlarmMatching::removePartner);

        List<AlarmMatching> matchingByRoom = alarmMatchingRepository.findAllBySignalRoomIn(rooms);
        matchingByRoom.forEach(AlarmMatching::removeSignalRoom);

        // 7. SignalRoom 삭제 (version 필드 있음 → 영속화해서 삭제)
        List<Long> roomIds = rooms.stream().map(SignalRoom::getId).toList();
        List<SignalRoom> managedRooms = signalRoomRepository.findAllById(roomIds);
        signalRoomRepository.deleteAll(managedRooms);

        // 8. 기타 관련 삭제
        userInterestsRepository.deleteAllByUser(user);
        tuningResultRepository.deleteAllByMatchedUser(user);

        // 9. 마지막으로 user 삭제
        userRepository.delete(user);
    }

    @Transactional
    public void deleteAllUsers() {
        signalMessageRepository.deleteAll();
        tuningReportUserReactionRepository.deleteAll();
        tuningReportRepository.deleteAll();
        userInterestsRepository.deleteAll();
        tuningResultRepository.deleteAll();
        tuningRepository.deleteAll();
        userAlarmRepository.deleteAll();
        alarmRepository.deleteAll();
        signalRoomRepository.deleteAll();
        userRepository.deleteAll();
    }
}


