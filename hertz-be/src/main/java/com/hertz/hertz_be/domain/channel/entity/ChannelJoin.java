package com.hertz.hertz_be.domain.channel.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channel_join")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelJoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_room_id", nullable = false)
    private Long channelRoomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
