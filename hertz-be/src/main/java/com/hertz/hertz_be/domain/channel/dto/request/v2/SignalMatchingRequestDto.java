package com.hertz.hertz_be.domain.channel.dto.request.v2;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignalMatchingRequestDto {

    @NotBlank
    private Long channelRoomId;
}
