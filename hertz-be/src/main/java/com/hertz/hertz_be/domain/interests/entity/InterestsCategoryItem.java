package com.hertz.hertz_be.domain.interests.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interests_category_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestsCategoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private InterestsCategory category;

    @Column(length = 20, nullable = false)
    private String name;
}
