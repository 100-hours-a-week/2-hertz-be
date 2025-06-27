package com.hertz.hertz_be.domain.alarm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@Getter
@Table(name = "alarm_alert")
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue("ALERT")
public class AlarmAlert extends Alarm {

    @Column(nullable = false, name = "report_message")
    private String reportedMessage;
}
