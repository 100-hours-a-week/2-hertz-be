package com.hertz.hertz_be.domain.channel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SendSignalRequestDto {

    @NotNull
    private Long receiverUserId;

    @NotBlank
    private String message;
}
