package com.hertz.hertz_be.domain.channel.controller.v1;

import com.hertz.hertz_be.domain.channel.dto.request.v1.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.TuningResponseDto;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.v1.ChannelService;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController("channelControllerV1")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "튜닝/채널 관련 v1 API")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/tuning/signal")
    @Operation(summary = "시그널 보내기 API")
    public ResponseEntity<ResponseDto<SendSignalResponseDto>> sendSignal(@RequestBody @Valid SendSignalRequestDto requestDTO,
                                                                         @AuthenticationPrincipal Long userId) {
        SendSignalResponseDto response = channelService.sendSignal(userId, requestDTO);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ChannelResponseCode.SIGNAL_ROOM_CREATED.getCode(), ChannelResponseCode.SIGNAL_ROOM_CREATED.getMessage(), response)
        );

    }

    @GetMapping("/tuning")
    @Operation(summary = "튜닝된 상대 반환 API")
    public ResponseEntity<ResponseDto<TuningResponseDto>> getTunedUser(@AuthenticationPrincipal Long userId) {
        TuningResponseDto response = channelService.getTunedUser(userId);
        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.NO_TUNING_CANDIDATE.getCode(), ChannelResponseCode.NO_TUNING_CANDIDATE.getMessage(), null));
        }
        return ResponseEntity.ok(
                new ResponseDto<>(ChannelResponseCode.TUNING_SUCCESS.getCode(), ChannelResponseCode.TUNING_SUCCESS.getMessage(), response)
        );
    }

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

    @PostMapping("/channel-rooms/{channelRoomId}/messages")
    @Operation(summary = "채널방 메세지 전송 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> sendChannelMessage(@PathVariable Long channelRoomId,
                                                                                  @AuthenticationPrincipal Long userId,
                                                                                  @RequestBody SendSignalRequestDto response) {

        channelService.sendChannelMessage(channelRoomId, userId, response);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(ChannelResponseCode.MESSAGE_CREATED.getCode(), ChannelResponseCode.MESSAGE_CREATED.getMessage(), null)
        );
    }
}
