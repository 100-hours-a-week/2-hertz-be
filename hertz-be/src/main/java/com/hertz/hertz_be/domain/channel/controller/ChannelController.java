package com.hertz.hertz_be.domain.channel.controller;

import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.request.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.TuningResponseDto;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.global.common.ResponseCode;
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
                new ResponseDto<>(ResponseCode.SIGNAL_ROOM_CREATED, "시그널 룸이 성공적으로 생성되었습니다.", response)
        );

    }

    @GetMapping("/v1/tuning")
    @Operation(summary = "튜닝된 상대 반환 API")
    public ResponseEntity<ResponseDto<TuningResponseDto>> getTunedUser(@AuthenticationPrincipal Long userId) {
        TuningResponseDto response = channelService.getTunedUser(userId);
        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.NO_TUNING_CANDIDATE, "추천 가능한 상대가 현재 없습니다.", null));
        }
        return ResponseEntity.ok(
                new ResponseDto<>(ResponseCode.TUNING_SUCCESS, "튜닝된 사용자가 정상적으로 조회되었습니다.", response)
        );
    }

    @GetMapping("/v1/channel")
    @Operation(summary = "개인 채널보관함 목록 반환 API")
    public ResponseEntity<ResponseDto<ChannelListResponseDto>> getPersonalSignalRoomList(@AuthenticationPrincipal Long userId,
                                                                                         @RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "10") int size) {

        ChannelListResponseDto response = channelService.getPersonalSignalRoomList(userId, page, size);

        if (response == null) {
            return ResponseEntity.ok(new ResponseDto<>(ResponseCode.NO_CHANNEL_ROOM, "참여 중인 채널이 없습니다.", null));
        }
        return ResponseEntity.ok(new ResponseDto<>(ResponseCode.CHANNEL_ROOM_LIST_FETCHED, "채널방 목록이 정상적으로 조회되었습니다.", response));
    }

    @GetMapping("/v1/channel-rooms/{channelRoomId}")
    @Operation(summary = "특정 채널방 반환 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> getChannelRoomMessages(@PathVariable Long channelRoomId,
                                                                                      @AuthenticationPrincipal Long userId,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size) {

        ChannelRoomResponseDto response = channelService.getChannelRoom(channelRoomId, userId, page, size);
        return ResponseEntity.ok(new ResponseDto<>("CHANNEL_ROOM_SUCCESS", "채널방이 정상적으로 조회되었습니다.", response));
    }

    @PostMapping("/v1/channel-rooms/{channelRoomId}/messages")
    @Operation(summary = "채널방 메세지 전송 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> sendChannelMessage(@PathVariable Long channelRoomId,
                                                                                  @AuthenticationPrincipal Long userId,
                                                                                  @RequestBody SendSignalRequestDto response) {

        channelService.sendChannelMessage(channelRoomId, userId, response);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(ResponseCode.MESSAGE_CREATED, "메세지가 성공적으로 전송되었습니다.", null)
        );
    }


    @PostMapping("/v2/matching/acceptances")
    @Operation(summary = "채널방 매칭 수락 API")
    public ResponseEntity<ResponseDto<Void>> channelMatchingAccept(@AuthenticationPrincipal Long userId,
                                                                                     @RequestBody SignalMatchingRequestDto response) {

        String matchingResult = channelService.channelMatchingStatusUpdate(userId, response, MatchingStatus.MATCHED);

        return switch (matchingResult) {
            case ResponseCode.MATCH_FAILED -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ResponseDto<>(ResponseCode.MATCH_FAILED, "상대방이 매칭을 거절했습니다.", null));
            case ResponseCode.MATCH_SUCCESS -> ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto<>(ResponseCode.MATCH_SUCCESS, "매칭이 성사되었습니다.", null));
            case ResponseCode.MATCH_PENDING -> ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new ResponseDto<>(ResponseCode.MATCH_PENDING, "상대방의 응답을 기다리는 중입니다.", null));
            case ResponseCode.USER_DEACTIVATED -> ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(new ResponseDto<>(ResponseCode.USER_DEACTIVATED, "상대방이 탈퇴한 사용자입니다.", null));
            default -> ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto<>(ResponseCode.INTERNAL_SERVER_ERROR, "알 수 없는 오류입니다.", null));
        };
    }


    @PostMapping("/v2/matching/rejections")
    @Operation(summary = "채널방 매칭 거절 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> channelMatchingReject(@AuthenticationPrincipal Long userId,
                                                                                     @RequestBody SignalMatchingRequestDto response) {

        return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto<>(
                            channelService.channelMatchingStatusUpdate(userId, response, MatchingStatus.UNMATCHED)
                            , "매칭 거절이 완료되었습니다."
                            , null));
    }

    @DeleteMapping("/v2/channel-rooms/{channelRoomId}")
    @Operation(summary = "채널방 나가기 API")
    public ResponseEntity<ResponseDto<Void>> leaveChannelRoom(@PathVariable Long channelRoomId,
                                                              @AuthenticationPrincipal Long userId) {
        channelService.leaveChannelRoom(channelRoomId, userId);
        return ResponseEntity.ok(new ResponseDto<>(ResponseCode.CHANNEL_ROOM_EXIT_SUCCESS, "채널방에서 정상적으로 나갔습니다.", null));
    }
}
