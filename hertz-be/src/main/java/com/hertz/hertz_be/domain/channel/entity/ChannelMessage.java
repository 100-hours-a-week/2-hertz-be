package com.hertz.hertz_be.domain.channel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_room_id", nullable = false)
    private Long channelRoomId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "message", nullable = false, length = 300)
    private String message;

    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;
}
