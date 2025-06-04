package com.hertz.hertz_be.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@Getter
@Table(name = "alarm_report")
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue("REPORT")
public class AlarmReport extends Alarm{

    @Column(nullable = false)
    private int couple_count;
}
