package com.hertz.hertz_be.domain.alarm.controller;

import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(name = "알림창 관련 API")
@RequestMapping("/api")
public class AlarmController {

    private final AlarmService alarmService;

    @PostMapping("/v2/alarms/notice")
    @Operation(summary = "공지 알림 작성 API")
    public ResponseEntity<ResponseDto<Void>> createNotifyAlarm(@RequestBody @Valid CreateNotifyAlarmRequestDto requestDto,
                                                               @AuthenticationPrincipal Long userId) {
        alarmService.createNotifyAlarm(requestDto, userId);
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ResponseCode.NOTICE_CREATED_SUCCESS, "공지가 성공적으로 등록되었습니다.", null)
        );
    }

}
