package com.hertz.hertz_be.domain.channel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignalMessageRequestDto {
    @NotBlank(message = "메세지를 입력해주세요.")
    private String message;
}
