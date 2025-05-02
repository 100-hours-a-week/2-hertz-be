package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signal_room")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiverUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_matching_status", nullable = false, length = 15)
    private String senderMatchingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_matching_status", nullable = false, length = 15)
    private String receiverMatchingStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

