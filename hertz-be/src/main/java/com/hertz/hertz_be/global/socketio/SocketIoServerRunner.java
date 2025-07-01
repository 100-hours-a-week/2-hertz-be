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
        try {
            server.start();
            log.info("✅ Socket.IO 서버 시작됨 (port={})", server.getConfiguration().getPort());
        } catch (Exception e) {
            if (e.getCause() instanceof java.net.BindException) {
                log.warn("⚠️ 이미 해당 포트로 Socket.IO 서버가 실행 중입니다. 무시하고 진행합니다.");
            } else {
                log.error("❌ Socket.IO 서버 시작 중 예외 발생", e);
            }
        }
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