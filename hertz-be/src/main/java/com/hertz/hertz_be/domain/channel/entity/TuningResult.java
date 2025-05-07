package com.hertz.hertz_be.domain.channel.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tuning_result")
public class TuningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuning_id", nullable = false, unique = true)
    private Tuning tuning;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_user_id", nullable = false)
    private User matchedUser;

    @Column(nullable = false)
    private Long lineup;
}
