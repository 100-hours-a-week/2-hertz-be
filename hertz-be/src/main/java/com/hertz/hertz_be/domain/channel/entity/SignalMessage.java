package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signal_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_room_id", nullable = false)
    private SignalRoom signalRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUserId;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;
}
