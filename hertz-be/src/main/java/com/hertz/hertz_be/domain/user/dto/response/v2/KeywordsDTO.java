package com.hertz.hertz_be.domain.user.dto.response.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordsDTO {
    private String mbti;
    private String religion;
    private String smoking;
    private String drinking;
}
