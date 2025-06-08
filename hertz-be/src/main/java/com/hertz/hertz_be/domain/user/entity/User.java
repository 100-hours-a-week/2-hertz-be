package com.hertz.hertz_be.domain.user.entity;

import com.hertz.hertz_be.domain.alarm.entity.AlarmNotification;
import com.hertz.hertz_be.domain.alarm.entity.UserAlarm;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import com.hertz.hertz_be.domain.user.entity.enums.MembershipType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter // Todo: Setter가 있으면 객체지향면에서 좋지 않기 때문에 제거하는게 좋을 것 같아요
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 생성자를 만들되 외부에서 접근하지 못 하도록 함
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", nullable = false, length = 10)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "profile_image_url", nullable = false, length = 512)
    private String profileImageUrl;

    @Column(nullable = false, length = 10, unique = true)
    private String nickname;

    @Column(name = "one_line_introduction", nullable = false, length = 100)
    private String oneLineIntroduction;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "membership_type", nullable = false, length = 25)
    private MembershipType membershipType = MembershipType.GENERAL_USER;

    @Builder.Default
    @Column(name = "is_friend_allowed", nullable = false)
    private Boolean isFriendAllowed = true;

    @Builder.Default
    @Column(name = "is_couple_allowed", nullable = false)
    private Boolean isCoupleAllowed = true;

    @Builder.Default
    @Column(name = "is_meal_friend_allowed", nullable = false)
    private Boolean isMealFriendAllowed = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserOauth userOauth;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "senderUser")
    @Builder.Default
    private List<SignalRoom> sentSignalRooms = new ArrayList<>();

    @OneToMany(mappedBy = "receiverUser")
    @Builder.Default
    private List<SignalRoom> receivedSignalRooms = new ArrayList<>();

    @OneToMany(mappedBy = "senderUser")
    @Builder.Default
    private List<SignalMessage> sendMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<Tuning> recommendListByCategory = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<UserAlarm> alarms = new ArrayList<>();

    @OneToMany(mappedBy = "writer")
    @Builder.Default
    private List<AlarmNotification> wroteNotifyAlarms = new ArrayList<>();
  
    public static User of(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

}
