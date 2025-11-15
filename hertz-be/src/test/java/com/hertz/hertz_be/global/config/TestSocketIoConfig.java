package com.hertz.hertz_be.global.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.hertz.hertz_be.global.socketio.CustomJsonSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSocketIoConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketIOServer socketIOServer() {
        System.out.println("# TestSocketIoConfig - socketIOServer #");
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        config.setOrigin("*");
        config.setAllowCustomRequests(true);
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);

        // 커스텀 JSON 처리기
        config.setJsonSupport(new CustomJsonSupport());

        System.out.println("## Socket.IO test server started on port 9092");
        return new SocketIOServer(config);
    }
}


