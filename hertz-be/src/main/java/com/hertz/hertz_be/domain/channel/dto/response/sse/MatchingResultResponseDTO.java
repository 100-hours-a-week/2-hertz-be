package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingResultResponseDTO {
    private Long partnerId;
    private String partnerProfileImage;
    private String partnerNickname;
}
