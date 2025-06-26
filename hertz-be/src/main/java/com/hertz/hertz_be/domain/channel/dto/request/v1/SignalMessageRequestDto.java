package com.hertz.hertz_be.domain.channel.dto.request.v1;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignalMessageRequestDto {
    @NotBlank(message = "메세지를 입력해주세요.")
    private String message;
}
