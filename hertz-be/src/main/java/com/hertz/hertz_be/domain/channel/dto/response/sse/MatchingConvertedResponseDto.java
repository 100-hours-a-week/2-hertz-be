package com.hertz.hertz_be.domain.channel.dto.response.sse;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.common.ResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchingConvertedResponseDto {

    private Long channelRoomId;
    private LocalDateTime matchedAt;
    private Long partnerId;
    private String partnerNickname;

}
