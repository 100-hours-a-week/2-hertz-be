package com.hertz.hertz_be.domain.channel.dto.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserMessageCountDto {
    private final Long userId;
    private final Long messageCount;
}
