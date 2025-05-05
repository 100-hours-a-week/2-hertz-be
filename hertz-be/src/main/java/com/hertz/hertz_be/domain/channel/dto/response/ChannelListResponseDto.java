package com.hertz.hertz_be.domain.channel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChannelListResponseDto {
    private List<ChannelSummaryDto> list;
    private int pageNumber;
    private int pageSize;
    private boolean isLast;
}
