package com.hertz.hertz_be.domain.channel.dto.response.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChannelListResponseDto {
    private List<ChannelSummaryDto> list;
    private int pageNumber;
    private int pageSize;
    @JsonProperty("isLast")
    private boolean isLast;
}
