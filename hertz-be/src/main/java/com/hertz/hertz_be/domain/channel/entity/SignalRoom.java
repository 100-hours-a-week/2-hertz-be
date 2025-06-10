package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.alarm.entity.AlarmMatching;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
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

    @Version
    private Long version;

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

    @OneToOne(mappedBy = "signalRoom", fetch = FetchType.LAZY)
    private TuningReport tuningReport;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "receiver_exited_at")
    @Builder.Default
    private LocalDateTime receiverExitedAt = null;

    @Column(name = "sender_exited_at")
    @Builder.Default
    private LocalDateTime senderExitedAt = null;

    @OneToMany(mappedBy = "signalRoom")
    @Builder.Default
    private List<SignalMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "signalRoom")
    @Builder.Default
    private List<AlarmMatching> alarms = new ArrayList<>();

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
            return MatchingStatus.MATCHED.getValue();
        } else if (senderMatchingStatus == MatchingStatus.UNMATCHED || receiverMatchingStatus == MatchingStatus.UNMATCHED) {
            return MatchingStatus.UNMATCHED.getValue();
        }
        return MatchingStatus.SIGNAL.getValue();
    }

    /**
     * 현재 유저가 SignalRoom을 나갔는지 아닌지 여부
     */
    public boolean isUserExited(Long userId) {
        boolean isSender = senderUser.getId().equals(userId);
        LocalDateTime exitedAt = isSender ? senderExitedAt : receiverExitedAt;
        return exitedAt != null;
    }

    /**
     * 현재 유저 SignalRoom을 나가기
     */
    public void leaveChannelRoom(Long userId) {
        boolean isSender = senderUser.getId().equals(userId);
        if (isSender) {
            if (this.senderExitedAt != null) return;
            this.senderExitedAt = LocalDateTime.now();
        } else {
            if (this.receiverExitedAt != null) return;
            this.receiverExitedAt = LocalDateTime.now();
        }
    }
}

