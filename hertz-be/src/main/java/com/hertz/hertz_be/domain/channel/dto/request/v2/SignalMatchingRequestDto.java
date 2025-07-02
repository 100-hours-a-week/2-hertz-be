package com.hertz.hertz_be.domain.channel.dto.request.v2;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SignalMatchingRequestDto {

    @NotNull
    private Long channelRoomId;
}
