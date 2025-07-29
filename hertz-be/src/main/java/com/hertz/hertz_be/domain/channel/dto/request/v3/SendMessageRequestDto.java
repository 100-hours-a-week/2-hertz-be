package com.hertz.hertz_be.domain.channel.dto.request.v3;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SendMessageRequestDto {

    @NotBlank
    private String message;

}
