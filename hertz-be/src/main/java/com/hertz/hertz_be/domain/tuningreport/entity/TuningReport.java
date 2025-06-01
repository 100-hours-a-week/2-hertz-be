package com.hertz.hertz_be.domain.tuningreport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLDelete(sql = "UPDATE tuning_report SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Table(name = "tuning_report")
public class TuningReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
