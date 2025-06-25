package com.hertz.hertz_be.domain.user.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.user.dto.response.v2.InterestsDTO;
import com.hertz.hertz_be.domain.user.dto.response.v2.KeywordsDTO;
import com.hertz.hertz_be.domain.user.dto.response.v2.UserProfileDTO;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service("userServiceV2")
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final InterestsService interestsService;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long targetUserId, Long userId) {
        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()));

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> keywordsMap = interestsService.getUserKeywords(targetUser.getId());
        Map<String, List<String>> currentUserInterestsMap = interestsService.getUserInterests(userId);

        KeywordsDTO keywordsDto = objectMapper.convertValue(keywordsMap, KeywordsDTO.class);
        InterestsDTO currentUserDto = objectMapper.convertValue(currentUserInterestsMap, InterestsDTO.class);


        if(Objects.equals(targetUser.getId(), userId)) { // 마이페이지 조회
            return new UserProfileDTO(
                    targetUser.getProfileImageUrl(),
                    targetUser.getNickname(),
                    targetUser.getGender(),
                    targetUser.getOneLineIntroduction(),
                    "ME",
                    keywordsDto,
                    currentUserDto,
                    null
            );
        } else { // 상대방 페이지 조회
            String relationType = userRepository.findRelationTypeBetweenUsers(userId, targetUser.getId());

            Map<String, List<String>> targetInterestsMap = interestsService.getUserInterests(targetUser.getId());
            Map<String, List<String>> sameInterestsMap = interestsService.extractSameInterests(targetInterestsMap, currentUserInterestsMap);
            InterestsDTO sameInterestsDto = objectMapper.convertValue(sameInterestsMap, InterestsDTO.class);

            return new UserProfileDTO(
                    targetUser.getProfileImageUrl(),
                    targetUser.getNickname(),
                    targetUser.getGender(),
                    targetUser.getOneLineIntroduction(),
                    relationType,
                    keywordsDto,
                    currentUserDto,
                    sameInterestsDto
            );
        }
    }
}
