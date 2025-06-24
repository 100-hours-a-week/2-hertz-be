package com.hertz.hertz_be.domain.channel.controller.v3;

import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.v3.ChannelService;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController("channelControllerV3")
@RequestMapping("/api/v3")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "튜닝/채널 관련 v3 API")
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping("/channel")
    @Operation(summary = "개인 채널보관함 목록 반환 API")
    public ResponseEntity<ResponseDto<ChannelListResponseDto>> getPersonalSignalRoomList(@AuthenticationPrincipal Long userId,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "10") int size) {

        ChannelListResponseDto response = channelService.getPersonalSignalRoomList(userId, page, size);

        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.NO_CHANNEL_ROOM.getCode(), ChannelResponseCode.NO_CHANNEL_ROOM.getMessage(), null));
        }
        return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.CHANNEL_ROOM_LIST_FETCHED.getCode(), ChannelResponseCode.CHANNEL_ROOM_LIST_FETCHED.getMessage(), response));
    }

    @GetMapping("/channel-rooms/{channelRoomId}")
    @Operation(summary = "특정 채널방 반환 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> getChannelRoomMessages(@PathVariable Long channelRoomId,
                                                                                      @AuthenticationPrincipal Long userId,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size) {

        ChannelRoomResponseDto response = channelService.getChannelRoom(channelRoomId, userId, page, size);
        return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.CHANNEL_ROOM_SUCCESS.getCode(), ChannelResponseCode.CHANNEL_ROOM_SUCCESS.getMessage(), response));
    }
}
