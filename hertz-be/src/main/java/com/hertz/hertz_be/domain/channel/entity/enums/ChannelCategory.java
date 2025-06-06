package com.hertz.hertz_be.domain.channel.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelCategory {
    FRIEND("친구", "FRIEND"),
    COUPLE("커플", "COUPLE");

    private final String label;
    private final String value;
}
