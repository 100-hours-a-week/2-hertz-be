package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.infra.ai.client.TuningAiClient;
import com.hertz.hertz_be.global.infra.ai.dto.AiTuningReportGenerationRequest;
import com.hertz.hertz_be.global.infra.ai.support.UserDataAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TuningReportProcessor implements ItemProcessor<SignalRoom, TuningReport> {

    private final TuningReportRepository tuningReportRepository;
    private final TuningAiClient tuningAiClient;
    private final SignalMessageRepository signalMessageRepository;
    private final UserRepository userRepository;
    private final UserDataAssembler userDataAssembler;

    @Override
    public TuningReport process(SignalRoom signalRoom) {
        // 이미 튜닝 리포트가 존재하면 skip
        Optional<TuningReport> existing = tuningReportRepository.findBySignalRoom(signalRoom);
        if (existing.isPresent()) return null;

        int chatCounts = signalMessageRepository.countBySignalRoom(signalRoom);

        // AI 요청
        AiTuningReportGenerationRequest request = AiTuningReportGenerationRequest.of(
                signalRoom,
                chatCounts,
                userDataAssembler.assemble(signalRoom.getSenderUser().getId()),
                userDataAssembler.assemble(signalRoom.getReceiverUser().getId())
        );
        Map<String, Object> response = tuningAiClient.requestTuningReport(request);
        String userEmailDomain = userRepository.findDistinctEmailDomains(signalRoom.getSenderUser().getId());

        // 응답 → Entity 변환
        return TuningReport.of(signalRoom, userEmailDomain, response);
    }
}