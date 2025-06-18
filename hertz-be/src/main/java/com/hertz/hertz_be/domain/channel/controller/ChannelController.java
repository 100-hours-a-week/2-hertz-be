package com.hertz.hertz_be.domain.channel.controller;

import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.request.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.TuningResponseDto;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.common.NewResponseCode;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "튜닝/채널 관련 API")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/v1/tuning/signal")
    @Operation(summary = "시그널 보내기 API")
    public ResponseEntity<ResponseDto<SendSignalResponseDto>> sendSignal(@RequestBody @Valid SendSignalRequestDto requestDTO,
                                                                         @AuthenticationPrincipal Long userId) {
        SendSignalResponseDto response = channelService.sendSignal(userId, requestDTO);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ChannelResponseCode.SIGNAL_ROOM_CREATED.getCode(), ChannelResponseCode.SIGNAL_ROOM_CREATED.getMessage(), response)
        );

    }

    @GetMapping("/v1/tuning")
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

    @GetMapping("/v1/channel")
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

    @GetMapping("/v1/channel-rooms/{channelRoomId}")
    @Operation(summary = "특정 채널방 반환 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> getChannelRoomMessages(@PathVariable Long channelRoomId,
                                                                                      @AuthenticationPrincipal Long userId,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size) {

        ChannelRoomResponseDto response = channelService.getChannelRoom(channelRoomId, userId, page, size);
        return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.CHANNEL_ROOM_SUCCESS.getCode(), ChannelResponseCode.CHANNEL_ROOM_SUCCESS.getMessage(), response));
    }

    @PostMapping("/v1/channel-rooms/{channelRoomId}/messages")
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


    @PostMapping("/v2/matching/acceptances")
    @Operation(summary = "채널방 매칭 수락 API")
    public ResponseEntity<ResponseDto<Void>> channelMatchingAccept(
            @AuthenticationPrincipal Long userId,
            @RequestBody SignalMatchingRequestDto response) {

        String matchingResult = channelService.channelMatchingStatusUpdate(userId, response, MatchingStatus.MATCHED);

        if (ChannelResponseCode.MATCH_FAILED.getCode().equals(matchingResult)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ResponseDto<>(ChannelResponseCode.MATCH_FAILED.getCode(), ChannelResponseCode.MATCH_FAILED.getMessage(), null));
        } else if (ChannelResponseCode.MATCH_SUCCESS.getCode().equals(matchingResult)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto<>(ChannelResponseCode.MATCH_SUCCESS.getCode(), ChannelResponseCode.MATCH_SUCCESS.getMessage(), null));
        } else if (ChannelResponseCode.MATCH_PENDING.getCode().equals(matchingResult)) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new ResponseDto<>(ChannelResponseCode.MATCH_PENDING.getCode(), ChannelResponseCode.MATCH_PENDING.getMessage(), null));
        } else if (UserResponseCode.USER_DEACTIVATED.getCode().equals(matchingResult)) {
            return ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(new ResponseDto<>(UserResponseCode.USER_DEACTIVATED.getCode(), UserResponseCode.USER_DEACTIVATED.getMessage(), null));
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto<>(NewResponseCode.INTERNAL_SERVER_ERROR.getCode(), NewResponseCode.INTERNAL_SERVER_ERROR.getMessage(), null));
        }
    }


    @PostMapping("/v2/matching/rejections")
    @Operation(summary = "채널방 매칭 거절 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> channelMatchingReject(@AuthenticationPrincipal Long userId,
                                                                                     @RequestBody SignalMatchingRequestDto response) {

        return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto<>(
                            channelService.channelMatchingStatusUpdate(userId, response, MatchingStatus.UNMATCHED)
                            , ChannelResponseCode.MATCH_REJECTION_SUCCESS.getMessage()
                            , null));
    }

    @DeleteMapping("/v2/channel-rooms/{channelRoomId}")
    @Operation(summary = "채널방 나가기 API")
    public ResponseEntity<ResponseDto<Void>> leaveChannelRoom(@PathVariable Long channelRoomId,
                                                              @AuthenticationPrincipal Long userId) {
        channelService.leaveChannelRoom(channelRoomId, userId);
        return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.CHANNEL_ROOM_EXIT_SUCCESS.getCode(), ChannelResponseCode.CHANNEL_ROOM_EXIT_SUCCESS.getMessage(), null));
    }
}
