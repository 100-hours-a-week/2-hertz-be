package com.hertz.hertz_be.domain.channel.dto.request.v1;

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
