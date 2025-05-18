package com.hertz.hertz_be.domain.interests.service;

import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.repository.TuningRepository;
import com.hertz.hertz_be.domain.interests.dto.request.UserAiInterestsRequestDto;
import com.hertz.hertz_be.domain.interests.dto.request.UserInterestsRequestDto;
import com.hertz.hertz_be.domain.interests.entity.InterestsCategory;
import com.hertz.hertz_be.domain.interests.entity.InterestsCategoryItem;
import com.hertz.hertz_be.domain.interests.entity.UserInterests;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import com.hertz.hertz_be.domain.interests.exception.*;
import com.hertz.hertz_be.domain.interests.repository.InterestsCategoryItemRepository;
import com.hertz.hertz_be.domain.interests.repository.InterestsCategoryRepository;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.exception.AiServerErrorException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterestsService {

    private final UserInterestsRepository userInterestsRepository;
    private final InterestsCategoryRepository interestsCategoryRepository;
    private final InterestsCategoryItemRepository interestsCategoryItemRepository;
    private final TuningRepository tuningRepository;
    private final UserRepository userRepository;
    private final WebClient webClient;

    @Autowired
    public InterestsService(UserRepository userRepository,
                            InterestsCategoryRepository interestsCategoryRepository,
                            InterestsCategoryItemRepository interestsCategoryItemRepository,
                            TuningRepository tuningRepository,
                            UserInterestsRepository userInterestsRepository,
                            @Value("${ai.server.ip}") String aiServerIp) {
        this.userInterestsRepository = userInterestsRepository;
        this.interestsCategoryRepository = interestsCategoryRepository;
        this.interestsCategoryItemRepository = interestsCategoryItemRepository;
        this.userRepository = userRepository;
        this.tuningRepository = tuningRepository;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
    }

    private Map<String, Object> requestAiBody = new HashMap<>();

    @Transactional
    public void saveUserInterests(UserInterestsRequestDto userInterestsRequestDto, Long userId) throws Exception {

        Map<String, String> keywordsMap = userInterestsRequestDto.getKeywords().toMap();
        Map<String, List<String>> interestsMap = userInterestsRequestDto.getInterests().toMap();
        Map<String, String> requestAiKeywordsBody = new HashMap<>(); // 키워드 담을 Map
        Map<String, String[]> requestAiInterestsBody = new HashMap<>(); // 관심사 담을 Map

        // 키워드, 관심사 null 및 공백 체크
        validateUserInterestsInput(userInterestsRequestDto.getKeywords().toMap(), userInterestsRequestDto.getInterests().toMap());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다.", ResponseCode.BAD_REQUEST));

        // 같은 도메인(조직)에 속한 모든 유저의 캐싱된 튜닝 리스트 초기화
        resetCachingTuningResult(user);

        requestAiBody.put("userId", user.getId());
        requestAiBody.put("emailDomain", extractDomainFromEmail(user.getEmail()));
        requestAiBody.put("gender", user.getGender());
        requestAiBody.put("ageGroup", user.getAgeGroup());

        try {
            // 키워드 처리
            keywordsMap.forEach((categoryName, itemName) -> {
                saveSingleUserInterest(user, InterestsCategoryType.KEYWORD, categoryName, itemName);
                requestAiKeywordsBody.put(categoryName, itemName);
            });

            // 관심사 처리
            interestsMap.forEach((categoryName, itemNames) -> {
                if(itemNames == null) {
                    throw new UserException("관심사 항목에 null 값이 있습니다",  ResponseCode.BAD_REQUEST);
                }
                itemNames.forEach(itemName -> {
                    saveSingleUserInterest(user, InterestsCategoryType.INTEREST, categoryName, itemName);
                    requestAiInterestsBody.put(categoryName, itemNames.toArray(new String[0]));
                });
            });
        } catch (Exception e) {
            throw new UserException("취향 등록 처리에 문제가 발생했습니다.", ResponseCode.BAD_REQUEST);
        }

        Map<String, Object> responseMap = saveInterestsToAiServer(requestAiKeywordsBody, requestAiInterestsBody);
        String code = (String) responseMap.get("code");

        switch (code) {
            case ResponseCode.EMBEDDING_REGISTER_SUCCESS -> { // 201
                return;
            }

            case ResponseCode.EMBEDDING_REGISTER_BAD_REQUEST -> { // 400
                throw new RegisterBadRequestException(code);
            }

            case ResponseCode.EMBEDDING_CONFLICT_DUPLICATE_ID -> { // 409
                throw new DuplicateIdException();
            }

            case ResponseCode.BAD_REQUEST_VALIDATION_ERROR -> { // 422
                throw new InvalidException();
            }

            case ResponseCode.EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED -> { // 500
                throw new SimilarityUpdateFailedException();
            }

            case ResponseCode.EMBEDDING_REGISTER_SERVER_ERROR -> { // 500
                throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
            }

            default -> {
                throw new RegisterBadRequestException(code);
            }
        }

    }

    private void saveSingleUserInterest(User user, InterestsCategoryType categoryType, String categoryName, String itemName) {
        try {
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
        } catch (Exception e) {
            throw new UserException("단일 취향 아이템 저장에 문제가 발생했습니다.", ResponseCode.BAD_REQUEST);
        }

    }

    private Map<String, Object> saveInterestsToAiServer(Map<String, String> keywordMap, Map<String, String[]> interestsMap) {
        String uri = "/api/v1/users";
        UserAiInterestsRequestDto aiRequest = UserAiInterestsRequestDto.builder()
                .userId((Long) requestAiBody.get("userId"))
                .emailDomain((String) requestAiBody.get("emailDomain"))
                .gender(String.valueOf(requestAiBody.get("gender")))
                .ageGroup(String.valueOf(requestAiBody.get("ageGroup")))
                .MBTI(keywordMap.get("mbti"))
                .religion(keywordMap.get("religion"))
                .smoking(keywordMap.get("smoking"))
                .drinking(keywordMap.get("drinking"))
                .personality(interestsMap.get("personality"))
                .preferredPeople(interestsMap.get("preferredPeople"))
                .currentInterests(interestsMap.get("currentInterests"))
                .favoriteFoods(interestsMap.get("favoriteFoods"))
                .likedSports(interestsMap.get("likedSports"))
                .pets(interestsMap.get("pets"))
                .selfDevelopment(interestsMap.get("selfDevelopment"))
                .hobbies(interestsMap.get("hobbies"))
                .build();

        Map<String, Object> responseMap = webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON) // JSON 형식으로 보내겠다고 명시
                .bodyValue(aiRequest) // requestBody를 POST 요청 본문에 담음
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (responseMap == null || !responseMap.containsKey("code")) {
            throw new AiServerErrorException(ResponseCode.EMBEDDING_REGISTER_SERVER_ERROR);
        }
        return responseMap;
    }

    public void validateUserInterestsInput(Map<String, String> keywordsMap, Map<String, List<String>> interestsMap) {
        // 1. keywordsMap: null 또는 공백 체크
        for (Map.Entry<String, String> entry : keywordsMap.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty()) {
                throw new InvalidInterestsInputException();
            }
        }

        // 2. interestsMap: 리스트 자체 or 내부 요소가 null/공백인지 체크
        for (Map.Entry<String, List<String>> entry : interestsMap.entrySet()) {
            List<String> list = entry.getValue();

            if (list == null || list.isEmpty()) {
                throw new InvalidInterestsInputException();
            }

            for (String item : list) {
                if (item == null || item.trim().isEmpty()) {
                    throw new InvalidInterestsInputException();
                }
            }
        }
    }

    public void resetCachingTuningResult(User user) {
        List<User> users = findUsersByEmailDomain(user);
        for (User oneUser : users) {
            clearTuningResultsOfUser(oneUser);
        }
    }

    private List<User> findUsersByEmailDomain(User user) {
        String emailDomain = extractDomainFromEmail(user.getEmail());
        return userRepository.findAllByEmailDomain(emailDomain);
    }

    private String extractDomainFromEmail(String email) {
        return email.split("@")[1];
    }

    private void clearTuningResultsOfUser(User user) {
        List<Tuning> tunings = user.getRecommendListByCategory();
        for (Tuning tuning : tunings) {
            tuning.getTuningResults().clear();
        }
    }
}
