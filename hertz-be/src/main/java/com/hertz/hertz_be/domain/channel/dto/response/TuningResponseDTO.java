package com.hertz.hertz_be.domain.channel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TuningResponseDTO {
    private Long userId;
    private String profileImage;
    private String nickname;
    private String gender;
    private String oneLineIntroduction;
    private Map<String, String> keywords;
    private Map<String, List<String>> sameInterests;
}
