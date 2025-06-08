package com.hertz.hertz_be.domain.alarm.controller;

import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
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
        return ResponseEntity.status(201).body(
                new ResponseDto<>(ResponseCode.NOTICE_CREATED_SUCCESS, "공지가 성공적으로 등록되었습니다.", null)
        );
    }

    @GetMapping("/v2/alarms")
    @Operation(summary = "특정 사용자를 위한 최근 30일 동안 모든 알림 반환 API")
    public ResponseEntity<ResponseDto<AlarmListResponseDto>> getAlarmList(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @AuthenticationPrincipal Long userId) {
        AlarmListResponseDto dto = alarmService.getAlarmList(page, size, userId);
        if (dto.list().isEmpty()) {
            return ResponseEntity.ok(new ResponseDto<>(
                    ResponseCode.NO_ALARMS, "최근 30일 동안 새로운 알림이 없습니다.", dto));
        }
        return ResponseEntity.ok(new ResponseDto<>(
                ResponseCode.ALARM_FETCH_SUCCESS, "알림이 정상적으로 조회되었습니다.", dto));
    }

    @DeleteMapping("/v2/users/alarms/{alarmId}")
    @Operation(summary = "특정 알림 삭제 API")
    public  ResponseEntity<ResponseDto<Void>> deleteAlarm(@PathVariable Long alarmId, @AuthenticationPrincipal Long userId) {
        alarmService.deleteAlarm(alarmId, userId);
        return ResponseEntity.ok(new ResponseDto<>(
                ResponseCode.ALARM_DELETE_SUCCESS, "알림이 성공적으로 삭제되었습니다.", null));
    }

}
