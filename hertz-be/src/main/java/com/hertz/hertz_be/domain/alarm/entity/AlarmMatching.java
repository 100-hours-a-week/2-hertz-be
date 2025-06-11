package com.hertz.hertz_be.domain.alarm.entity;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@Getter
@Table(name = "alarm_matching")
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue("MATCHING")
public class AlarmMatching extends Alarm{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private User partner;

    @Column(name = "partner_nickname", nullable = false)
    private String partnerNickname;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "signal_room_id", nullable = true)
    private SignalRoom signalRoom;

    @Column(name = "is_matched", nullable = false)
    private boolean isMatched;

    public void removePartner() {
        this.partner = null;
    }

    public void removeSignalRoom() {
        this.signalRoom = null;
    }
}
