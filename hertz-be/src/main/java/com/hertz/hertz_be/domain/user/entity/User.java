package com.hertz.hertz_be.domain.user.entity;

import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import com.hertz.hertz_be.domain.user.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    @Column(name = "status_code", nullable = false, length = 25)
    private Status statusCode;

    @Column(name = "is_friend_allowed", nullable = false)
    private Boolean isFriendAllowed;

    @Column(name = "is_couple_allowed", nullable = false)
    private Boolean isCoupleAllowed;

    @Column(name = "is_meal_friend_allowed", nullable = false)
    private Boolean isMealFriendAllowed;

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
}
