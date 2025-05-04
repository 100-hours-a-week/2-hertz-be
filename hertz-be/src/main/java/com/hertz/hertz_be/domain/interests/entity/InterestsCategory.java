package com.hertz.hertz_be.domain.interests.entity;

import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interests_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestsCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", length = 10, nullable = false)
    private InterestsCategoryType categoryType;
}