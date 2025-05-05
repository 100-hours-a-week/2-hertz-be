package com.hertz.hertz_be.domain.channel.controller;


import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.dto.response.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDTO;
import com.hertz.hertz_be.domain.channel.dto.response.TuningResponseDTO;
import com.hertz.hertz_be.domain.channel.service.ChannelService;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/v1/tuning/signal")
    public ResponseEntity<ResponseDto<SendSignalResponseDTO>> sendSignal(
            @RequestBody @Valid SendSignalRequestDTO requestDTO, @AuthenticationPrincipal Long userId) {
        SendSignalResponseDTO response = channelService.sendSignal(userId, requestDTO);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ResponseCode.SIGNAL_ROOM_CREATED, "시그널 룸이 성공적으로 생성되었습니다.", response)
        );
    }

    @GetMapping("/v1/tuning")
    public ResponseEntity<ResponseDto<TuningResponseDTO>> getTunedUser(@AuthenticationPrincipal Long userId) {
        TuningResponseDTO response = channelService.getTunedUser(userId);
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
    public ResponseEntity<ResponseDto<ChannelListResponseDto>> getPersonalChannelList(@AuthenticationPrincipal Long userId,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        ChannelListResponseDto response = channelService.getPersonalChannelList(userId, page, size);

        if(response == null) {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.CHANNEL_ROOM_LIST_FETCHED, "채널방 목록이 정상적으로 조회되었습니다.", response)
            );
        } else {
            return ResponseEntity.ok(
                    new ResponseDto<>(ResponseCode.NO_CHANNEL_ROOM, "참여 중인 채널이 없습니다.", response)
            );
        }



    }
}
