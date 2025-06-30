package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SocketIoServerRunner {

    private final SocketIOServer server;

    public SocketIoServerRunner(SocketIOServer server) {
        this.server = server;
    }

    @PostConstruct
    public void startServer() {
        server.start();
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.getAllClients().forEach(client -> {
                client.disconnect();
                log.info("🔌 클라이언트 연결 해제: sessionId = {}", client.getSessionId());
            });
            server.stop();
            log.info("🧼 Socket.IO 서버 종료 완료");
        }
    }
}