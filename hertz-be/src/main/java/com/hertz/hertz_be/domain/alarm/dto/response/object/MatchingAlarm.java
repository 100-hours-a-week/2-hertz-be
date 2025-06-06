package com.hertz.hertz_be.domain.alarm.dto.response.object;

public record MatchingAlarm(
        String type,
        String title,
        Long channelRoomId,
        String createdDate
) implements AlarmItem {}
