package com.hertz.hertz_be.domain.channel.dto.request.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SendSignalRequestDto {

    @NotNull
    private Long receiverUserId;

    @NotBlank
    private String message;
}
