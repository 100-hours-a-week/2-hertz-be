package com.hertz.hertz_be.global.infra.ai.client;

import com.hertz.hertz_be.domain.channel.dto.request.v3.ChatReportRequestDto;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.AiServerBadRequestException;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.infra.ai.dto.request.AiTuningReportGenerationRequest;
import com.hertz.hertz_be.global.infra.ai.dto.response.AiChatReportResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TuningAiClient {

    @Value("${ai.tuningreport.ip}")
    private String AI_TUNING_REPORT_IP;
    private final WebClient.Builder webClientBuilder;
    private final WebClient tuningWebClient;

    public Map<String, Object> requestTuningReport(AiTuningReportGenerationRequest aiReportRequest) {
        WebClient webClient = webClientBuilder.baseUrl(AI_TUNING_REPORT_IP).build();
        String uri = "api/v2/report";

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

    public Map<String, Object> requestTuningByCategory(Long userId, String category) {
        String uri = "/api/v3/tuning?userId=" + userId + "&category=" + category;

        Map<String, Object> responseMap = tuningWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (responseMap == null || !responseMap.containsKey("code")) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "튜닝 과정에서 AI 서버 오류 발생했습니다."
            );
        }

        return responseMap;
    }

    public AiChatReportResponseDto sendChatReport(ChatReportRequestDto request) {
        String uri = "/api/v3/chat/report";

        try {
            return tuningWebClient
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiChatReportResponseDto.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "메세지 신고 과정에서 AI 서버 API 요청 오류 발생했습니다."
            );
        }
    }
}
