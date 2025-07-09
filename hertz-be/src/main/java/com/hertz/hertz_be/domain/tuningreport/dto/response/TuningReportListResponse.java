package com.hertz.hertz_be.domain.tuningreport.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TuningReportListResponse {
    private List<ReportItem> list;
    private int pageNumber;
    private int pageSize;

    @JsonProperty("isLast") // JSON에선 isLast 유지
    private boolean isLast;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReportItem {
        private LocalDateTime createdDate;
        private Long reportId;
        private String title;
        private String content;
        private Reactions reactions;
        private MyReactions myReactions;

        @JsonCreator
        public ReportItem(
                @JsonProperty("createdDate") LocalDateTime createdDate,
                @JsonProperty("reportId") Long reportId,
                @JsonProperty("title") String title,
                @JsonProperty("content") String content,
                @JsonProperty("reactions") Reactions reactions,
                @JsonProperty("myReactions") MyReactions myReactions
        ) {
            this.createdDate = createdDate;
            this.reportId = reportId;
            this.title = title;
            this.content = content;
            this.reactions = reactions;
            this.myReactions = myReactions;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reactions {
        private int celebrate;
        private int thumbsUp;
        private int laugh;
        private int eyes;
        private int heart;

        public void increase(ReactionType type) {
            switch (type) {
                case CELEBRATE -> celebrate++;
                case THUMBS_UP -> thumbsUp++;
                case LAUGH -> laugh++;
                case EYES -> eyes++;
                case HEART -> heart++;
            }
        }

        public void decrease(ReactionType type) {
            switch (type) {
                case CELEBRATE -> celebrate = Math.max(0, celebrate - 1);
                case THUMBS_UP -> thumbsUp = Math.max(0, thumbsUp - 1);
                case LAUGH -> laugh = Math.max(0, laugh - 1);
                case EYES -> eyes = Math.max(0, eyes - 1);
                case HEART -> heart = Math.max(0, heart - 1);
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyReactions {
        private boolean celebrate;
        private boolean thumbsUp;
        private boolean laugh;
        private boolean eyes;
        private boolean heart;

        public void set(ReactionType type, boolean value) {
            switch (type) {
                case CELEBRATE -> celebrate = value;
                case THUMBS_UP -> thumbsUp = value;
                case LAUGH -> laugh = value;
                case EYES -> eyes = value;
                case HEART -> heart = value;
            }
        }
    }
}
