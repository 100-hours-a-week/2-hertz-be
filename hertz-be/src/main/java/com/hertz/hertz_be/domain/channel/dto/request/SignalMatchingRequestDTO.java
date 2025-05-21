package com.hertz.hertz_be.domain.channel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignalMatchingRequestDTO {

    @NotBlank
    private Long channelRoomId;
}
