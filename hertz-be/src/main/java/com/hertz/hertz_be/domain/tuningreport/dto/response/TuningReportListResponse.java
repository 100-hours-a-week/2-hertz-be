package com.hertz.hertz_be.domain.tuningreport.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TuningReportListResponse(
        List<ReportItem> list,
        int pageNumber,
        int pageSize,
        boolean isLast
) {
        public record ReportItem(
                LocalDateTime createdDate,
                Long reportId,
                String title,
                String content,
                Reactions reactions,
                MyReactions myReactions
        ) {
            public record Reactions(
                    int celebrate,
                    int thumbsUp,
                    int laugh,
                    int eyes,
                    int heart
            ) {}

            public record MyReactions(
                    boolean celebrate,
                    boolean thumbsUp,
                    boolean laugh,
                    boolean eyes,
                    boolean heart
            ) {}
        }
    }

