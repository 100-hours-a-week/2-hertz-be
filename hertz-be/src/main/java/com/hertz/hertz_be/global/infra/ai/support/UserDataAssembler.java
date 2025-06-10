package com.hertz.hertz_be.global.infra.ai.support;

import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.infra.ai.dto.AiTuningReportGenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserDataAssembler {

    private final UserRepository userRepository;
    private final UserInterestsRepository userInterestsRepository;

    public AiTuningReportGenerationRequest.UserData assemble(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        Map<String, List<String>> keywordsMap = userInterestsRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(
                        ui -> ui.getCategoryItem().getCategory().getCategoryType().name(),
                        Collectors.mapping(ui -> ui.getCategoryItem().getName(), Collectors.toList())
                ));
        Map<String, List<String>> interestsMap = userInterestsRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(
                        ui -> ui.getCategoryItem().getCategory().getName(),
                        Collectors.mapping(ui -> ui.getCategoryItem().getName(), Collectors.toList())
                ));


        List<String> keywords = keywordsMap.getOrDefault("KEYWORD", List.of());
        //List<List<String>> interests = categorizedInterests.getOrDefault("INTEREST", List.of());

        String mbti = keywords.size() > 0 ? keywords.get(0) : null;
        String religion = keywords.size() > 1 ? keywords.get(1) : null;
        String smoking = keywords.size() > 2 ? keywords.get(2) : null;
        String drinking = keywords.size() > 3 ? keywords.get(3) : null;

        return new AiTuningReportGenerationRequest.UserData(
                user.getGender().name(),
                user.getEmail(),
                mbti,
                religion,
                smoking,
                drinking,
                interestsMap.getOrDefault("personality", List.of()),
                interestsMap.getOrDefault("preferredPeople", List.of()),
                interestsMap.getOrDefault("currentInterests", List.of()),
                interestsMap.getOrDefault("favoriteFoods", List.of()),
                interestsMap.getOrDefault("likedSports", List.of()),
                interestsMap.getOrDefault("pets", List.of()),
                interestsMap.getOrDefault("selfDevelopment", List.of()),
                interestsMap.getOrDefault("hobbies", List.of())
        );
    }
}
