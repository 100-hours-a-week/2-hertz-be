package com.hertz.hertz_be.domain.alarm.dto.response.object;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoticeAlarm.class, name = "NOTICE"),
        @JsonSubTypes.Type(value = ReportAlarm.class, name = "REPORT"),
        @JsonSubTypes.Type(value = MatchingAlarm.class, name = "MATCHING")
})
public sealed interface AlarmItem permits NoticeAlarm, ReportAlarm, MatchingAlarm {
    String type();
    String title();
    String createdDate();
}
