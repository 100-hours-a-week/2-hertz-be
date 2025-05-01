package com.hertz.hertz_be.domain.interests.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestsRequestDto {

    @NotNull
    @Valid
    private Keywords keywords;

    @NotNull
    @Valid
    private Interests interests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Keywords {

        @NotBlank
        private String MBTI;

        @NotBlank
        private String religion;

        @NotBlank
        private String smoking;

        @NotBlank
        private String drinking;

        public Map<String, String> toMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("MBTI", MBTI);
            map.put("religion", religion);
            map.put("smoking", smoking);
            map.put("drinking", drinking);
            return map;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interests {

        @Size(min = 1, max = 10)
        private List<String> personality;

        @Size(min = 1, max = 10)
        private List<String> preferredPeople;

        @Size(min = 1, max = 10)
        private List<String> currentInterests;

        @Size(min = 1, max = 10)
        private List<String> favoriteFoods;

        @Size(min = 1, max = 10)
        private List<String> likedSports;

        @Size(min = 1, max = 10)
        private List<String> pets;

        @Size(min = 1, max = 10)
        private List<String> selfDevelopment;

        @Size(min = 1, max = 10)
        private List<String> hobbies;

        public Map<String, List<String>> toMap() {
            Map<String, List<String>> map = new LinkedHashMap<>();
            map.put("personality", personality);
            map.put("preferredPeople", preferredPeople);
            map.put("currentInterests", currentInterests);
            map.put("favoriteFoods", favoriteFoods);
            map.put("likedSports", likedSports);
            map.put("pets", pets);
            map.put("selfDevelopment", selfDevelopment);
            map.put("hobbies", hobbies);
            return map;
        }
    }
}
