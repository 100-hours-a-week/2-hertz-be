package com.hertz.hertz_be.global.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.global.common.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", ResponseCode.UNAUTHORIZED);
        errorResponse.put("message", "지정한 리소스에 대한 액세스 권한이 없습니다.");
        errorResponse.put("data", null);

        String json = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
}
