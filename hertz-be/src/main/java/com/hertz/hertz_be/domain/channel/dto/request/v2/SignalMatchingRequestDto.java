package com.hertz.hertz_be.domain.channel.dto.request.v2;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignalMatchingRequestDto {

    @NotNull
    private Long channelRoomId;
}
