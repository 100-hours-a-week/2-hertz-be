package com.hertz.hertz_be.domain.alarm.dto.response.object;

public record ReportAlarm(
        String type,
        String title,
        String createdDate
) implements AlarmItem {}
