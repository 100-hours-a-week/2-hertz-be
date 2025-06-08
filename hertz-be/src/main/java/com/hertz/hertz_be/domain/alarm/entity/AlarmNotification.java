package com.hertz.hertz_be.domain.alarm.entity;

import com.hertz.hertz_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.w3c.dom.Text;

@Entity
@SuperBuilder
@Getter
@Table(name = "alarm_notification")
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue("NOTICE")
public class AlarmNotification extends Alarm{

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = true)
    private User writer;

    public void removeWriter() {
        this.writer = null;
    }
}
