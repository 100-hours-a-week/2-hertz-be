package com.hertz.hertz_be.domain.alarm.entity;

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
}
