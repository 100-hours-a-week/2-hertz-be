package com.hertz.hertz_be.domain.channel.dto.response;

import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import java.util.List;
import java.util.Map;

public record TuningResponseDto(
        Long userId,
        String profileImage,
        String nickname,
        Gender gender,
        String oneLineIntroduction,
        Map<String, String> keywords,
        Map<String, List<String>> sameInterests
) {}
