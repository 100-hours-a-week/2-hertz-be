package com.hertz.hertz_be.global.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.socketio.CustomJsonSupport;
import com.hertz.hertz_be.global.util.SocketIoTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.corundumstudio.socketio.AuthorizationResult;

@Configuration
@RequiredArgsConstructor
public class SocketIoConfig {

    @Value("${socketio.server.hostname}")
    private String hostname;

    @Value("${socketio.server.port}")
    private int port;

    private final JwtTokenProvider jwtTokenProvider;
    private final SocketIoTokenUtil socketIoTokenUtil;

    @Bean
    public SocketIOServer socketIoServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(hostname);
        config.setPort(port);
        config.setOrigin("*");
        config.setAllowCustomRequests(true);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        // 커스텀 JSON 처리기
        config.setJsonSupport(new CustomJsonSupport());

        config.setAuthorizationListener(handshakeData -> {
            String cookie = handshakeData.getHttpHeaders().get("cookie");
            String refreshToken = socketIoTokenUtil.extractCookie(cookie, "refreshToken");

            if(refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            }
            return AuthorizationResult.FAILED_AUTHORIZATION;
        });

        return new SocketIOServer(config);
    }
}