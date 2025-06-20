package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TuningReportVisibilityWriter implements ItemWriter<TuningReport> {

    private final AlarmService alarmService;
    private final TuningReportRepository tuningReportRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends TuningReport> chunk) {
        String emailDomain = chunk.getItems().getFirst().getEmailDomain();
        int coupleCount = chunk.size();

        for (TuningReport report : chunk.getItems()) {
            report.setVisible();
            tuningReportRepository.save(report);
        }

        alarmService.createTuningReportAlarm(emailDomain, coupleCount);
    }
}
