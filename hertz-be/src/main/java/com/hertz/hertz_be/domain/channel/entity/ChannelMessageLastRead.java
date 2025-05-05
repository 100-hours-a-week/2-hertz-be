package com.hertz.hertz_be.domain.channel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_message_last_read")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelMessageLastRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "last_message_read_id", nullable = false)
    private Long lastMessageReadId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;
}
