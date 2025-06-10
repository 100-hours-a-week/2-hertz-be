package com.hertz.hertz_be.global.infra.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.global.exception.AiServerBadRequestException;
import com.hertz.hertz_be.global.infra.ai.dto.AiTuningReportGenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TuningAiClient {

    @Value("${ai.tuningreport.ip}")
    private String AI_TUNING_REPORT_IP;
    private static WebClient webClient;

    public TuningAiClient(@Value("${ai.tuningreport.ip}") String aiTuningReportIp) {
        this.webClient = WebClient.builder().baseUrl(aiTuningReportIp).build();
    }

    public Map<String, Object> requestTuningReport(AiTuningReportGenerationRequest aiReportRequest) {
        String uri = AI_TUNING_REPORT_IP + "api/v2/report";

        try{
            return webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(aiReportRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            throw new AiServerBadRequestException();
        }
    }
}
