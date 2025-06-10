package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TuningReportWriter implements ItemWriter<TuningReport> {

    private final TuningReportRepository tuningReportRepository;

    @Override
    public void write(Chunk<? extends TuningReport> chunk) throws Exception {
        if(chunk.isEmpty()) return;

        // signal_room 데이터가 아니라 AI 튜닝 리포트 값이 들어와야 함
        tuningReportRepository.saveAll(chunk.getItems());
    }
}
