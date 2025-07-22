package com.hertz.hertz_be.domain.alarm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateNotifyAlarmRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
