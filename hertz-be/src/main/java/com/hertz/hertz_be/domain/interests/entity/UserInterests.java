package com.hertz.hertz_be.domain.interests.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_interests",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "category_item_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_item_id", nullable = false)
    private InterestsCategoryItem categoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 너의 기존 user 엔티티가 존재한다고 가정
}