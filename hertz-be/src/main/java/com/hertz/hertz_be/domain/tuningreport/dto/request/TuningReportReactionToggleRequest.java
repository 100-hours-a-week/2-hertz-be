package com.hertz.hertz_be.domain.tuningreport.dto.request;

import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record TuningReportReactionToggleRequest (
        @NotNull ReactionType reactionType
) {}
