package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.global.infra.ai.client.TuningAiClient;
import com.hertz.hertz_be.global.infra.ai.dto.request.AiTuningReportGenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional
public class TuningReportGenerationWriter implements ItemWriter<AiTuningReportGenerationRequest> {

    private final TuningReportRepository tuningReportRepository;
    private final TuningAiClient tuningAiClient;
    private final SignalRoomRepository signalRoomRepository;

    @Override
    public void write(Chunk<? extends AiTuningReportGenerationRequest> chunk) {
        if (chunk.isEmpty()) return;

        List<TuningReport> reports = chunk.getItems().stream()
                .map(request -> {
                    Long roomId = request.signalRoom().id();
                    SignalRoom room = signalRoomRepository.findById(roomId)
                            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

                    Map<String, Object> response = tuningAiClient.requestTuningReport(request);
                    return TuningReport.of(room, request.emailDomain(), response);
                })
                .toList();

        tuningReportRepository.saveAll(reports);
    }
}
