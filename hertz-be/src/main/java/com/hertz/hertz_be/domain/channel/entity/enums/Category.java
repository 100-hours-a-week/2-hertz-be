package com.hertz.hertz_be.domain.channel.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    FRIEND("친구"),
    COUPLE("커플"),
    MEAL_FRIEND("밥친구");

    private final String label;
}
