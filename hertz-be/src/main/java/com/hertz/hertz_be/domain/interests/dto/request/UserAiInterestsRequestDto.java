package com.hertz.hertz_be.domain.interests.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAiInterestsRequestDto {
    private Long userId;
    private String emailDomain;
    private String gender;
    private String ageGroup;

    // 키워드
    @JsonProperty("MBTI")
    private String MBTI;
    private String religion;
    private String smoking;
    private String drinking;

    // 관심사
    private String[] personality;
    private String[] preferredPeople;
    private String[] currentInterests;
    private String[] favoriteFoods;
    private String[] likedSports;
    private String[] pets;
    private String[] selfDevelopment;
    private String[] hobbies;
}
