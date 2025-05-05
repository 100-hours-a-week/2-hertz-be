package com.hertz.hertz_be.domain.channel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String category;

    @Column(name = "channel_name", length = 20)
    private String channelName;

    @Column(name = "channel_content", length = 100)
    private String channelContent;

    @Column(name = "current_user_count", nullable = false)
    private int currentUserCount;

    @Column(name = "max_user_count", nullable = false)
    private int maxUserCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
