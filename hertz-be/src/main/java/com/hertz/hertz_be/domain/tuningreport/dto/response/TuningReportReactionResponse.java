package com.hertz.hertz_be.domain.tuningreport.dto.response;

import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;

public record TuningReportReactionResponse (
        Long reportId,
        ReactionType reactionType,
        boolean isReacted,
        int reactionCount
) {}
