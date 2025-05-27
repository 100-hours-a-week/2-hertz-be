package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "signal_message",
        indexes = {
                @Index(name = "idx_signal_message_room_sendat", columnList = "signal_room_id, send_at DESC"),
                @Index(name = "idx_signal_room_id", columnList = "signal_room_id")
        }
)
public class SignalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_room_id", nullable = false)
    private SignalRoom signalRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;
}
