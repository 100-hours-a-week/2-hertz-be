package com.hertz.hertz_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestsDTO {
    private List<String> personality;
    private List<String> preferredPeople;
    private List<String> currentInterests;
    private List<String> favoriteFoods;
    private List<String> likedSports;
    private List<String> pets;
    private List<String> selfDevelopment;
    private List<String> hobbies;
}
