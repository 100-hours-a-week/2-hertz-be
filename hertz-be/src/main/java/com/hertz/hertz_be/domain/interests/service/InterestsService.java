package com.hertz.hertz_be.domain.interests.service;

import com.hertz.hertz_be.domain.interests.dto.request.UserInterestsRequestDto;
import com.hertz.hertz_be.domain.interests.entity.InterestsCategory;
import com.hertz.hertz_be.domain.interests.entity.InterestsCategoryItem;
import com.hertz.hertz_be.domain.interests.entity.UserInterests;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import com.hertz.hertz_be.domain.interests.repository.InterestsCategoryItemRepository;
import com.hertz.hertz_be.domain.interests.repository.InterestsCategoryRepository;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterestsService {

    private final UserInterestsRepository userInterestsRepository;
    private final InterestsCategoryRepository interestsCategoryRepository;
    private final InterestsCategoryItemRepository interestsCategoryItemRepository;
    private final UserRepository userRepository;
    //private final WebClient.Builder webClientBuilder;

    //@Value("${ai.server.ip}")
    //private String AI_SERVER_IP;

    @Transactional
    public void saveUserInterests(UserInterestsRequestDto userInterestsRequestDto, Long userId) {

        Map<String, String> keywordsMap = userInterestsRequestDto.getKeywords().toMap();
        Map<String, List<String>> interestsMap = userInterestsRequestDto.getInterests().toMap();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다.", ResponseCode.BAD_REQUEST));

        // 키워드 처리
        keywordsMap.forEach((categoryName, itemName) -> {
            saveSingleUserInterest(user, InterestsCategoryType.KEYWORD, categoryName, itemName);

        });

        // 관심사 처리
        interestsMap.forEach((categoryName, itemNames) -> {
            itemNames.forEach(itemName -> {
                saveSingleUserInterest(user, InterestsCategoryType.INTEREST, categoryName, itemName);
            });
        });

        // AI - 데이터 합치는 과정
//        EmbeddingRequestDto dto = EmbeddingRequestMapper.toDto(
//                user.getId(),
//                user.getEmail().split("@")[1],
//                user.getGender().name(),
//                user.getAgeGroup().name(),
//                keywordsMap.
//        );
//
//        // AI - 임베딩(회원 등록) API 연동
//        String aiResponse = webClientBuilder.build()
//                .post()
//                .uri(AI_SERVER_IP)
//                .bodyValue(dto)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        // 응답값 활용할 필요 있으면 여기에 처리
//        System.out.println("AI 응답: " + aiResponse);
    }

    private void saveSingleUserInterest(User user, InterestsCategoryType categoryType, String categoryName, String itemName) {
        InterestsCategory category = interestsCategoryRepository
                .findByCategoryTypeAndName(categoryType, categoryName)
                .orElseGet(() -> interestsCategoryRepository.save(
                        InterestsCategory.builder()
                                .categoryType(categoryType)
                                .name(categoryName)
                                .build()));

        InterestsCategoryItem categoryItem = interestsCategoryItemRepository
                .findByCategoryAndName(category, itemName)
                .orElseGet(() -> interestsCategoryItemRepository.save(
                        InterestsCategoryItem.builder()
                                .category(category)
                                .name(itemName)
                                .build()));

        boolean exists = userInterestsRepository.existsByUserAndCategoryItem(user, categoryItem);
        if (!exists) {
            userInterestsRepository.save(
                    UserInterests.builder()
                            .user(user)
                            .categoryItem(categoryItem)
                            .build());
        }
    }
}
