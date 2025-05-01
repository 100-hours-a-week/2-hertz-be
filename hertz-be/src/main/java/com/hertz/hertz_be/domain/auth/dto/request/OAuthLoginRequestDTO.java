package com.hertz.hertz_be.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequestDTO {
    @NotBlank
    private String code;

    @NotBlank
    private String state;
}
