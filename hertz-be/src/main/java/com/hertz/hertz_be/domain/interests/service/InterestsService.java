package com.hertz.hertz_be.domain.interests.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.channel.entity.Tuning;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InterestsService {

    private final UserInterestsRepository userInterestsRepository;
    private final InterestsCategoryRepository interestsCategoryRepository;
    private final InterestsCategoryItemRepository interestsCategoryItemRepository;
    private final UserRepository userRepository;
    private final WebClient webClient;
    private final RetryTemplate retryTemplate;

    @Autowired
    public InterestsService(UserRepository userRepository,
                            InterestsCategoryRepository interestsCategoryRepository,
                            InterestsCategoryItemRepository interestsCategoryItemRepository,
                            UserInterestsRepository userInterestsRepository,
                            @Value("${ai.server.ip}") String aiServerIp, RetryTemplate retryTemplate) {
        this.userInterestsRepository = userInterestsRepository;
        this.interestsCategoryRepository = interestsCategoryRepository;
        this.interestsCategoryItemRepository = interestsCategoryItemRepository;
        this.userRepository = userRepository;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
        this.retryTemplate = retryTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(InterestsService.class);

    @Transactional
    public void saveUserInterests(UserInterestsRequestDto userInterestsRequestDto, Long userId) throws Exception {
        retryTemplate.execute(retryContext -> {
            log.debug("🔥 [saveUserInterests] 취향 저장 시작 - userId: {}", userId);
            Map<String, String> keywordsMap = userInterestsRequestDto.getKeywords().toMap();
            Map<String, List<String>> interestsMap = userInterestsRequestDto.getInterests().toMap();
            validateUserInterestsInput(keywordsMap, interestsMap);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("❌ [saveUserInterests] 유저 없음 - userId: {}", userId);
                        return new UserException("사용자를 찾을 수 없습니다.", ResponseCode.BAD_REQUEST);
                    });
            log.debug("✅ [saveUserInterests] 유저 조회 완료 - email: {}", user.getEmail());

            resetCachingTuningResult(user);
            log.debug("🔄 [saveUserInterests] 캐싱 튜닝 결과 초기화");

            Map<String, Object> aiRequestBody = buildRequestAiBody(user);
            Map<String, String> aiKeywords = new HashMap<>();
            Map<String, String[]> aiInterests = new HashMap<>();

            try {
                saveKeywordInterests(user, keywordsMap, aiKeywords);
                saveInterestItems(user, interestsMap, aiInterests);
            } catch (Exception e) {
                log.error("❌ [saveUserInterests] 취향 저장 중 예외 발생", e);
                throw new UserException("취향 등록 처리에 문제가 발생했습니다.", ResponseCode.BAD_REQUEST);
            }


            // 트랜잭션 커밋 이후 실행
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        retryTemplate.execute(context -> {
                            log.debug("🚀 [saveUserInterests - TransactionSynchronizationManager] AI 서버에 요청 시작");
                            Map<String, Object> responseMap = saveInterestsToAiServer(aiRequestBody, aiKeywords, aiInterests);
                            log.debug("📥 [saveUserInterests - TransactionSynchronizationManager] AI 응답: {}", responseMap);
                            return null;
                        });
                    } catch (Exception e) {
                        log.error("❌ AI 서버 호출 실패", e);
                    }
                }
            });

            return null;
        });

    }

    private Map<String, Object> buildRequestAiBody(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", user.getId());
        body.put("emailDomain", extractDomainFromEmail(user.getEmail()));
        body.put("gender", user.getGender());
        body.put("ageGroup", user.getAgeGroup());
        return body;
    }

    private void saveKeywordInterests(User user, Map<String, String> keywordMap, Map<String, String> aiKeywords) {
        keywordMap.forEach((categoryName, itemName) -> {
            log.debug("📌 [saveKeywordInterests] 저장 - 카테고리: {}, 아이템: {}", categoryName, itemName);
            saveSingleUserInterest(user, InterestsCategoryType.KEYWORD, categoryName, itemName);
            aiKeywords.put(categoryName, itemName);
        });
    }

    private void saveInterestItems(User user, Map<String, List<String>> interestMap, Map<String, String[]> aiInterests) {
        for (Map.Entry<String, List<String>> entry : interestMap.entrySet()) {
            String categoryName = entry.getKey();
            List<String> itemNames = entry.getValue();

            if (itemNames == null) {
                throw new UserException("관심사 항목에 null 값이 있습니다", ResponseCode.BAD_REQUEST);
            }

            log.debug("📌 [saveInterestItems] 카테고리: {}, 항목 수: {}", categoryName, itemNames.size());
            itemNames.forEach(itemName ->
                    saveSingleUserInterest(user, InterestsCategoryType.INTEREST, categoryName, itemName)
            );

            aiInterests.put(categoryName, itemNames.toArray(new String[0]));
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

    private Map<String, Object> saveInterestsToAiServer(Map<String, Object> requestAiBody, Map<String, String> keywordMap, Map<String, String[]> interestsMap) {
        String uri = "/api/v1/users";
        Long userId = (Long) requestAiBody.get("userId");

        UserAiInterestsRequestDto aiRequest = UserAiInterestsRequestDto.builder()
                .userId(userId)
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

        try {
            return webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(aiRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        }catch (WebClientResponseException e) {
            log.warn("⚠️ [AI 서버 오류] status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                String body = e.getResponseBodyAsString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(body);
                String code = json.has("code") ? json.get("code").asText() : null;
              
                return switch (code) {
                    case ResponseCode.EMBEDDING_CONFLICT_DUPLICATE_ID -> {
                        log.warn("⚠️ 이미 등록된 유저. userId: {}", userId);
                        yield Map.of("code", ResponseCode.EMBEDDING_REGISTER_SUCCESS); // 정상처럼 처리
                    }
                    case ResponseCode.EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED -> throw new SimilarityUpdateFailedException();
                    case ResponseCode.EMBEDDING_REGISTER_SERVER_ERROR -> throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
                    case ResponseCode.BAD_REQUEST_VALIDATION_ERROR -> throw new InvalidException();
                    default -> throw new RegisterBadRequestException(code);
                };

            } catch (Exception parsingEx) {
                log.error("❌ AI 응답 파싱 실패", parsingEx);
                throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
            }
        }
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

    public Map<String, String> getUserKeywords(Long userId) {
        Map<String, String> keywords = userInterestsRepository.findByUserId(userId).stream()
                .filter(ui -> ui.getCategoryItem().getCategory().getCategoryType() == InterestsCategoryType.KEYWORD)
                .collect(
                        LinkedHashMap::new,
                        (map, ui) -> map.put(ui.getCategoryItem().getCategory().getName(), ui.getCategoryItem().getName()),
                        LinkedHashMap::putAll
                );

        return keywords;
    }

    public Map<String, List<String>> getUserInterests(Long userId) {
        Map<String, List<String>> interestsMap = new LinkedHashMap<>();

        userInterestsRepository.findByUserId(userId).stream()
                .filter(ui -> ui.getCategoryItem().getCategory().getCategoryType() == InterestsCategoryType.INTEREST)
                .forEach(ui -> {
                    String categoryName = ui.getCategoryItem().getCategory().getName();
                    String itemName = ui.getCategoryItem().getName();
                    interestsMap.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(itemName);
                });

        return interestsMap;
    }

    public Map<String, List<String>> extractSameInterests(Map<String, List<String>> interests1, Map<String, List<String>> interests2) {
        Map<String, List<String>> sameInterests = new LinkedHashMap<>();

        for (String category : interests1.keySet()) {
            List<String> list1 = interests1.getOrDefault(category, Collections.emptyList());
            List<String> list2 = interests2.getOrDefault(category, Collections.emptyList());

            // 교집합 추출
            Set<String> common = new HashSet<>(list1);
            common.retainAll(list2);

            // 값이 있으면 1개만 반환, 없으면 빈 리스트
            if (!common.isEmpty()) {
                sameInterests.put(category, List.of(common.iterator().next()));
            } else {
                sameInterests.put(category, Collections.emptyList());
            }
        }

        return sameInterests;
    }
}