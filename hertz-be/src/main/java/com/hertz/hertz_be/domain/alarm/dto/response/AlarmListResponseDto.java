package com.hertz.hertz_be.domain.alarm.dto.response;

import com.hertz.hertz_be.domain.alarm.dto.response.object.AlarmItem;

import java.util.List;

public record AlarmListResponseDto(
        List<AlarmItem> list,
        int pageNumber,
        int pageSize,
        boolean isLast
) {}
