package com.hertz.hertz_be.domain.tuningreport.entity;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE tuning_report SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Table(name = "tuning_report")
@Builder
public class TuningReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "signal_room_id", nullable = false, unique = true)
    private SignalRoom signalRoom;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "reaction_celebrate", nullable = false)
    @ColumnDefault("0")
    private int reactionCelebrate;

    @Column(name = "reaction_thumbs_up", nullable = false)
    @ColumnDefault("0")
    private int reactionThumbsUp;

    @Column(name = "reaction_laugh", nullable = false)
    @ColumnDefault("0")
    private int reactionLaugh;

    @Column(name = "reaction_eyes", nullable = false)
    @ColumnDefault("0")
    private int reactionEyes;

    @Column(name = "reaction_heart", nullable = false)
    @ColumnDefault("0")
    private int reactionHeart;

    @Column(name = "email_domain", nullable = false)
    private String emailDomain;

    @Column(name = "is_visible", nullable = false)
    @ColumnDefault("false")
    private boolean isVisible;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() { // 엔티티 저장 전 호출
        this.createdAt = this.modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { // 엔티티 수정 전 호출
        this.modifiedAt = LocalDateTime.now();
    }

    public void increaseReaction(ReactionType type) {
        switch (type) {
            case CELEBRATE -> this.reactionCelebrate++;
            case THUMBS_UP -> this.reactionThumbsUp++;
            case LAUGH -> this.reactionLaugh++;
            case EYES -> this.reactionEyes++;
            case HEART -> this.reactionHeart++;
        }
    }

    public void decreaseReaction(ReactionType type) {
        switch (type) {
            case CELEBRATE -> this.reactionCelebrate = Math.max(0, this.reactionCelebrate - 1);
            case THUMBS_UP -> this.reactionThumbsUp = Math.max(0, this.reactionThumbsUp - 1);
            case LAUGH -> this.reactionLaugh = Math.max(0, this.reactionLaugh - 1);
            case EYES -> this.reactionEyes = Math.max(0, this.reactionEyes - 1);
            case HEART -> this.reactionHeart = Math.max(0, this.reactionHeart - 1);
        }
    }

    public static TuningReport of(SignalRoom signalRoom, String emailDomain, Map<String, Object> response) {
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        return TuningReport.builder()
                .signalRoom(signalRoom)
                .title((String) dataMap.get("title"))
                .content((String) dataMap.get("content"))
                .emailDomain(emailDomain)
                .isVisible(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
