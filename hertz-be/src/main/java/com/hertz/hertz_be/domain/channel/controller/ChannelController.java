package com.hertz.hertz_be.domain.channel.controller;


import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDTO;
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
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/v1/tuning/signal")
    public ResponseEntity<ResponseDto<SendSignalResponseDTO>> sendSignal(
            @RequestBody @Valid SendSignalRequestDTO requestDTO, @AuthenticationPrincipal Long userId) {
        SendSignalResponseDTO response = channelService.sendSignal(userId, requestDTO);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ResponseCode.SIGNAL_ROOM_CREATED, "시그널 룸이 성공적으로 생성되었습니다.", response)
        );
    }
}
