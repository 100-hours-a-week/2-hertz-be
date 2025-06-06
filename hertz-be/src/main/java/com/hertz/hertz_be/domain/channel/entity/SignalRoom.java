package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "user_pair_signal", nullable = false, unique = true, length = 35)
    private String userPairSignal;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_matching_status", nullable = false, length = 15)
    private MatchingStatus senderMatchingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_matching_status", nullable = false, length = 15)
    private MatchingStatus receiverMatchingStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "signalRoom")
    @Builder.Default
    private List<SignalMessage> messages = new ArrayList<>();

    /**
     * 현재 유저 기준으로 상대방을 반환
     */
    public User getPartnerUser(Long currentUserId) {
        if (senderUser.getId().equals(currentUserId)) return receiverUser;
        if (receiverUser.getId().equals(currentUserId)) return senderUser;
        throw new IllegalArgumentException("해당 유저는 이 방의 참가자가 아닙니다.");
    }

    /**
     * 현재 유저가 이 방에 참가 중인지 여부
     */
    public boolean isParticipant(Long userId) {
        return senderUser.getId().equals(userId) || receiverUser.getId().equals(userId);
    }

    /**
     * RelationType을 반환하는 유틸
     */
    public String getRelationType() {
        if (senderMatchingStatus == MatchingStatus.MATCHED && receiverMatchingStatus == MatchingStatus.MATCHED) {
            return "MATCHING";
        } else if (senderMatchingStatus == MatchingStatus.SIGNAL && receiverMatchingStatus == MatchingStatus.SIGNAL) {
            return "SIGNAL";
        }// 상황에 따라 ENUM으로 바꿔도 됨
        return "UNMATCHING";
    }

}

