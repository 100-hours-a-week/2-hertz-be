package com.hertz.hertz_be.domain.channel.dto.request.v2;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignalMatchingRequestDto {

    @NotNull
    private Long channelRoomId;
}
