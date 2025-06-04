package com.hertz.hertz_be.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.w3c.dom.Text;

@Entity
@Builder
@Getter
@Table(name = "alarm_notification")
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Text content;
}
