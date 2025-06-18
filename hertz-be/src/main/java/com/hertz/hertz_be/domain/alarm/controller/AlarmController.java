package com.hertz.hertz_be.domain.alarm.controller;

import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.alarm.responsecode.AlarmResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity
                .status(AlarmResponseCode.NOTICE_CREATED_SUCCESS.getHttpStatus())
                .body(new ResponseDto<>(
                        AlarmResponseCode.NOTICE_CREATED_SUCCESS.getCode(),
                        AlarmResponseCode.NOTICE_CREATED_SUCCESS.getMessage(),
                        null
                ));
    }

    @GetMapping("/v2/alarms")
    @Operation(summary = "특정 사용자를 위한 최근 30일 동안 모든 알림 반환 API")
    public ResponseEntity<ResponseDto<AlarmListResponseDto>> getAlarmList(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @AuthenticationPrincipal Long userId) {
        AlarmListResponseDto dto = alarmService.getAlarmList(page, size, userId);
        if (dto.list().isEmpty()) {
            return ResponseEntity
                    .status(AlarmResponseCode.NO_ALARMS.getHttpStatus())
                    .body(new ResponseDto<>(
                            AlarmResponseCode.NO_ALARMS.getCode(),
                            AlarmResponseCode.NO_ALARMS.getMessage(),
                            dto
                    ));
        }
        return ResponseEntity
                .status(AlarmResponseCode.ALARM_FETCH_SUCCESS.getHttpStatus())
                .body(new ResponseDto<>(
                        AlarmResponseCode.ALARM_FETCH_SUCCESS.getCode(),
                        AlarmResponseCode.ALARM_FETCH_SUCCESS.getMessage(),
                        dto
                ));
    }

    @DeleteMapping("/v2/users/alarms/{alarmId}")
    @Operation(summary = "특정 알림 삭제 API")
    public ResponseEntity<ResponseDto<Void>> deleteAlarm(@PathVariable Long alarmId,
                                                         @AuthenticationPrincipal Long userId) {
        alarmService.deleteAlarm(alarmId, userId);
        return ResponseEntity
                .status(AlarmResponseCode.ALARM_DELETE_SUCCESS.getHttpStatus())
                .body(new ResponseDto<>(
                        AlarmResponseCode.ALARM_DELETE_SUCCESS.getCode(),
                        AlarmResponseCode.ALARM_DELETE_SUCCESS.getMessage(),
                        null
                ));
    }

}
