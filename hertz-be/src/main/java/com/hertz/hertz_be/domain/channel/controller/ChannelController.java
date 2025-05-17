package com.hertz.hertz_be.domain.channel.controller;

import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDTO;
import com.hertz.hertz_be.domain.channel.dto.response.TuningResponseDTO;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/v1/tuning/signal")
    public ResponseEntity<ResponseDto<SendSignalResponseDTO>> sendSignal(@RequestBody @Valid SendSignalRequestDTO requestDTO,
                                                                         @AuthenticationPrincipal Long userId) {
        SendSignalResponseDTO response = channelService.sendSignal(userId, requestDTO);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ResponseCode.SIGNAL_ROOM_CREATED, "시그널 룸이 성공적으로 생성되었습니다.", response)
        );

    }

    @GetMapping("/v1/tuning")
    public ResponseEntity<ResponseDto<TuningResponseDTO>> getTunedUser(@AuthenticationPrincipal Long userId) {
        TuningResponseDTO response = channelService.getTunedUser(userId);
        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.NO_TUNING_CANDIDATE, "추천 가능한 상대가 현재 없습니다.", null));
        }
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.TUNING_SUCCESS, "튜닝된 사용자가 정상적으로 조회되었습니다.", response)
        );
    }

    @GetMapping("/v1/new-messages")
    public ResponseEntity<ResponseDto<Void>> checkNewMessages(@AuthenticationPrincipal Long userId) {
        boolean hasNewMessage = channelService.hasNewMessages(userId);

        if (hasNewMessage) {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.NEW_MESSAGE, "새 메시지가 정상적으로 조회되었습니다.", null)
            );
        } else {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.NO_ANY_NEW_MESSAGE, "새 메시지가 없습니다.", null)
            );
        }
    }

    @GetMapping("/v1/channel")
    public ResponseEntity<ResponseDto<ChannelListResponseDto>> getPersonalSignalRoomList(@AuthenticationPrincipal Long userId,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "10") int size) {

        // Todo: 추후 시그널 -> 채널로 마이그레이션 시 메소드명 변경 필요 (getPersonalSignalRoomList -> getPersonalChannelList)
        ChannelListResponseDto response = channelService.getPersonalSignalRoomList(userId, page, size);

        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.NO_CHANNEL_ROOM, "참여 중인 채널이 없습니다.", null));
        }
        return ResponseEntity.ok(new ResponseDto<>(ResponseCode.CHANNEL_ROOM_LIST_FETCHED, "채널방 목록이 정상적으로 조회되었습니다.", response));
    }

    @GetMapping("/v1/channel-rooms/{channelRoomId}")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> getChannelRoomMessages(@PathVariable Long channelRoomId,
                                                                                      @AuthenticationPrincipal Long userId,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size) {

        ChannelRoomResponseDto response = channelService.getChannelRoomMessages(channelRoomId, userId, page, size);
        return ResponseEntity.ok(new ResponseDto<>("CHANNEL_ROOM_SUCCESS", "채널방이 정상적으로 조회되었습니다.", response));
    }

    @PostMapping("/v1/channel-rooms/{channelRoomId}/messages")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> sendChannelMessage(@PathVariable Long channelRoomId,
                                                                                  @AuthenticationPrincipal Long userId,
                                                                                  @RequestBody SendSignalRequestDTO response) {

        channelService.sendChannelMessage(channelRoomId, userId, response);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(ResponseCode.MESSAGE_CREATED, "메세지가 성공적으로 전송되었습니다.", null)
        );
    }
}
