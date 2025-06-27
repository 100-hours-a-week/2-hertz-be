package com.hertz.hertz_be.domain.channel.dto.request.v3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatReportRequestDto {

    @NotNull
    private int messageId;

    @NotBlank
    private String messageContent;

    @NotNull
    private int reportedUserId;
}
