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

    @Transactional
    public void saveUserInterests(UserInterestsRequestDto userInterestsRequestDto, Long userId) throws Exception {
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
                .findByCategoryTypeAndName(category, itemName)
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
