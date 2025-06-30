package com.hertz.hertz_be.domain.user.dto.request.v3;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class OneLineIntroductionRequestDto {
    @NotNull
    private String oneLineIntroduction;
}
