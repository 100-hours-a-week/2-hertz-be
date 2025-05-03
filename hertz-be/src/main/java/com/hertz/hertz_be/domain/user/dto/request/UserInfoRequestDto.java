package com.hertz.hertz_be.domain.user.dto.request;

import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoRequestDto {

    @NotBlank
    private String providerId;

    @NotBlank
    private String provider;

    @NotBlank
    private String profileImage;

    @NotBlank
    private String email;

    @NotBlank
    private String nickname;

    @NotNull
    private AgeGroup ageGroup;

    @NotNull
    private Gender gender;

    @NotBlank
    private String oneLineIntroduction;


}