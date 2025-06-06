package com.hertz.hertz_be.domain.alarm.dto.response.object;

public record NoticeAlarm(
        String type,
        String title,
        String content,
        String createdDate
) implements AlarmItem {}
