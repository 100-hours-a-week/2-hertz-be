package com.hertz.hertz_be.domain.channel.controller.v2;

import com.hertz.hertz_be.domain.channel.dto.request.v2.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.v2.ChannelService;
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

@RestController("channelControllerV2")
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "튜닝/채널 관련 v2 API")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/matching/acceptances")
    @Operation(summary = "채널방 매칭 수락 API")
    public ResponseEntity<ResponseDto<Void>> channelMatchingAccept(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SignalMatchingRequestDto response) {

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


    @PostMapping("/matching/rejections")
    @Operation(summary = "채널방 매칭 거절 API")
    public ResponseEntity<ResponseDto<ChannelRoomResponseDto>> channelMatchingReject(@AuthenticationPrincipal Long userId,
                                                                                     @RequestBody @Valid SignalMatchingRequestDto response) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(
                        channelService.channelMatchingStatusUpdate(userId, response, MatchingStatus.UNMATCHED)
                        , ChannelResponseCode.MATCH_REJECTION_SUCCESS.getMessage()
                        , null));
    }

    @DeleteMapping("/channel-rooms/{channelRoomId}")
    @Operation(summary = "채널방 나가기 API")
    public ResponseEntity<ResponseDto<Void>> leaveChannelRoom(@PathVariable Long channelRoomId,
                                                              @AuthenticationPrincipal Long userId) {
        channelService.leaveChannelRoom(channelRoomId, userId);
        return ResponseEntity.ok(new ResponseDto<>(ChannelResponseCode.CHANNEL_ROOM_EXIT_SUCCESS.getCode(), ChannelResponseCode.CHANNEL_ROOM_EXIT_SUCCESS.getMessage(), null));
    }
}
