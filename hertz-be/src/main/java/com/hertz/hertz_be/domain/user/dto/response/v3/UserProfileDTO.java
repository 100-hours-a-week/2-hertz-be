package com.hertz.hertz_be.domain.user.dto.response.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hertz.hertz_be.domain.user.dto.response.v3.InterestsDTO;
import com.hertz.hertz_be.domain.user.dto.response.v3.KeywordsDTO;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private String profileImage;
    private String nickname;
    private Gender gender;
    private String oneLineIntroduction;
    private String relationType;

    private KeywordsDTO keywords;
    private InterestsDTO interests;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InterestsDTO sameInterests;

    private Boolean friendAllowed;
    private Boolean coupleAllowed;

}
