package com.hertz.hertz_be.domain.interests.service;

import com.hertz.hertz_be.domain.channel.entity.Tuning;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

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

    private final Map<String, Object> requestAiBody = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(InterestsService.class);

    @Transactional
    public void saveUserInterests(UserInterestsRequestDto userInterestsRequestDto, Long userId) throws Exception {
        log.debug("🔥 [saveUserInterests] 취향 저장 시작 - userId: {}", userId);
        Map<String, String> keywordsMap = userInterestsRequestDto.getKeywords().toMap();
        Map<String, List<String>> interestsMap = userInterestsRequestDto.getInterests().toMap();
        Map<String, String> requestAiKeywordsBody = new HashMap<>();
        Map<String, String[]> requestAiInterestsBody = new HashMap<>();

        validateUserInterestsInput(keywordsMap, interestsMap);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ [saveUserInterests] 유저 없음 - userId: {}", userId);
                    return new UserException("사용자를 찾을 수 없습니다.", ResponseCode.BAD_REQUEST);
                });
        log.debug("✅ [saveUserInterests] 유저 조회 완료 - email: {}", user.getEmail());

        log.debug("🔄 [saveUserInterests] 캐싱 튜닝 결과 초기화");
        resetCachingTuningResult(user);

        requestAiBody.put("userId", user.getId());
        requestAiBody.put("emailDomain", extractDomainFromEmail(user.getEmail()));
        requestAiBody.put("gender", user.getGender());
        requestAiBody.put("ageGroup", user.getAgeGroup());
        log.debug("🧠 [saveUserInterests] AI 요청 바디 생성: {}", requestAiBody);

        try {
            keywordsMap.forEach((categoryName, itemName) -> {
                log.debug("📌 [saveUserInterests] 키워드 저장 - 카테고리: {}, 아이템: {}", categoryName, itemName);
                saveSingleUserInterest(user, InterestsCategoryType.KEYWORD, categoryName, itemName);
                requestAiKeywordsBody.put(categoryName, itemName);
            });

            interestsMap.forEach((categoryName, itemNames) -> {
                log.debug("📌 [saveUserInterests] 관심사 저장 시작 - 카테고리: {}, 아이템 수: {}", categoryName, itemNames.size());
                if (itemNames == null) {
                    throw new UserException("관심사 항목에 null 값이 있습니다", ResponseCode.BAD_REQUEST);
                }
                itemNames.forEach(itemName -> {
                    saveSingleUserInterest(user, InterestsCategoryType.INTEREST, categoryName, itemName);
                    requestAiInterestsBody.put(categoryName, itemNames.toArray(new String[0]));
                });
            });
        } catch (Exception e) {
            log.error("❌ [saveUserInterests] 취향 저장 중 예외 발생", e);
            throw new UserException("취향 등록 처리에 문제가 발생했습니다.", ResponseCode.BAD_REQUEST);
        }

        log.debug("🚀 [saveUserInterests] AI 서버에 요청 시작");
        Map<String, Object> responseMap = saveInterestsToAiServer(requestAiKeywordsBody, requestAiInterestsBody);
        log.debug("📥 [saveUserInterests] AI 응답: {}", responseMap);
        String code = (String) responseMap.get("code");

        switch (code) {
            case ResponseCode.EMBEDDING_REGISTER_SUCCESS -> {
                return;
            }
            case ResponseCode.EMBEDDING_REGISTER_BAD_REQUEST -> throw new RegisterBadRequestException(code);
            case ResponseCode.EMBEDDING_CONFLICT_DUPLICATE_ID -> throw new DuplicateIdException();
            case ResponseCode.BAD_REQUEST_VALIDATION_ERROR -> throw new InvalidException();
            case ResponseCode.EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED -> throw new SimilarityUpdateFailedException();
            case ResponseCode.EMBEDDING_REGISTER_SERVER_ERROR -> throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
            default -> throw new RegisterBadRequestException(code);
        }
    }

    private void saveSingleUserInterest(User user, InterestsCategoryType categoryType, String categoryName, String itemName) {

        try {
            log.debug("🔎 [saveSingleUserInterest] 저장 시도 - userId: {}, type: {}, category: {}, item: {}", user.getId(), categoryType, categoryName, itemName);
            InterestsCategory category = interestsCategoryRepository.findByCategoryTypeAndName(categoryType, categoryName)
                    .orElseGet(() -> {
                        try {
                            log.debug("🔎 [saveSingleUserInterest - Category] 저장 요청");
                            return interestsCategoryRepository.save(
                                    InterestsCategory.builder()
                                            .categoryType(categoryType)
                                            .name(categoryName)
                                            .build());

                        } catch (DataIntegrityViolationException e) {
                            return interestsCategoryRepository.findByCategoryTypeAndName(categoryType, categoryName)
                                    .orElseThrow(() -> {
                                        log.error("❌ [saveSingleUserInterest] 저장 중 예외 발생", e);
                                        return new RuntimeException("카테고리 중복 저장 실패");
                                    });
                        }
                    });
            log.debug("✅ [saveSingleUserInterest - Category] 저장 완료 - userId : {}", user.getId());
            InterestsCategoryItem categoryItem = interestsCategoryItemRepository.findByCategoryAndName(category, itemName)
                    .orElseGet(() -> {
                        try {
                            log.debug("🔎 [saveSingleUserInterest - CategoryItem] 저장 요청");
                            return interestsCategoryItemRepository.save(
                                    InterestsCategoryItem.builder()
                                            .category(category)
                                            .name(itemName)
                                            .build());
                        } catch (DataIntegrityViolationException e) {
                            return interestsCategoryItemRepository.findByCategoryAndName(category, itemName)
                                    .orElseThrow(() -> {
                                        log.error("❌ [saveSingleUserInterest - CategoryItem] 저장 중 예외 발생", e);
                                        return new RuntimeException("아이템 중복 저장 실패");
                                    });
                        }
                    });

            log.debug("✅ [saveSingleUserInterest - CategoryItem] 저장 완료 - userId : {}", user.getId());
            if (!userInterestsRepository.existsByUserAndCategoryItem(user, categoryItem)) {
                userInterestsRepository.save(UserInterests.builder()
                        .user(user)
                        .categoryItem(categoryItem)
                        .build());
            }
            log.debug("✅ [saveSingleUserInterest - CategoryItem] 저장 완료 - categoryItemId: {}", categoryItem.getId());
        } catch (Exception e) {
            log.error("❌ [saveSingleUserInterest] 저장 실패 - category: {}, item: {}", categoryName, itemName, e);
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
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(aiRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (responseMap == null || !responseMap.containsKey("code")) {
            throw new AiServerErrorException(ResponseCode.EMBEDDING_REGISTER_SERVER_ERROR);
        }
        return responseMap;
    }

    public void validateUserInterestsInput(Map<String, String> keywordsMap, Map<String, List<String>> interestsMap) {
        for (String value : keywordsMap.values()) {
            if (value == null || value.trim().isEmpty()) {
                throw new InvalidInterestsInputException();
            }
        }
        for (List<String> list : interestsMap.values()) {
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