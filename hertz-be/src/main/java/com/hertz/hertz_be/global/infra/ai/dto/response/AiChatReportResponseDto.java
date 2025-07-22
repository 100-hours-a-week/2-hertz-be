package com.hertz.hertz_be.global.infra.ai.dto.response;

import java.util.Map;

public record AiChatReportResponseDto (String code, Map<String, Object> data){
}
