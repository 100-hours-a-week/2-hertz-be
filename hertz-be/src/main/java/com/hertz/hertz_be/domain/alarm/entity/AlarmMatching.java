package com.hertz.hertz_be.domain.alarm.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.w3c.dom.Text;

@Entity
@Builder
@Getter
@Table(name = "alarm_matching")
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMatching {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User partner;

    @Column(name = "partner_nickname", nullable = false)
    private String partnerNickname;

    @Column(name = "is_matched", nullable = false)
    private boolean isMatched;
}
