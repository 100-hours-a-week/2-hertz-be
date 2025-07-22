package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.BindException;

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
            if (!serverRunning()) {
                server.start();
                log.info(":: [Socket.IO] Server - Start ::");
            } else {
                log.info(":: [Socket.IO] Server - already process ::");
            }
        } catch (Exception e) {
            if (containsBindException(e)) {
                log.warn(":: [Socket.IO] Server - Port conflict, Restart ::");

                // 강제 재시작 로직
                try {
                    server.stop(); // 이미 떠 있는 서버 종료 시도
                    server.start(); // 다시 시작
                    log.info(":: [Socket.IO] Server - Restart Success::");
                } catch (Exception ex) {
                    log.error(":: [Socket.IO] Server - Restart Fail::", ex);
                }
            } else {
                log.error(":: [Socket.IO] Server - Run Fail::", e);
            }
        }
    }

    private boolean serverRunning() {
        try {
            // 최소한 포트가 바인딩되어 있고, 클라이언트 목록을 가져올 수 있다면 서버는 살아있음
            return server.getConfiguration().getPort() > 0 && !server.getAllClients().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containsBindException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof BindException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
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