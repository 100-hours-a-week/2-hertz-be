package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageMarkRequest;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageRequest;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SocketIoController {
    private final SocketIOServer server;
    private final SocketIoService messageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SignalRoomRepository signalRoomRepository;

    public SocketIoController(SocketIOServer server, SocketIoService messageService, JwtTokenProvider jwtTokenProvider, SignalRoomRepository signalRoomRepository) {
        this.server = server;
        this.messageService = messageService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.signalRoomRepository = signalRoomRepository;

        server.addConnectListener(listenConnected());

        server.addEventListener("send_message", SocketIoMessageRequest.class, (client, data, ackSender) -> {
            Long senderId = getUserIdFromClient(client);

            log.info("[{}] 채널 {} 에게 메세지 : {}", senderId, data.roomId(), data.message());

            SignalMessage savedMessage = messageService.saveMessage(data.roomId(), senderId, data.message());
            server.getRoomOperations("room-" + data.roomId()).sendEvent("receive_message", SocketIoMessageResponse.from(savedMessage));
        });

        server.addEventListener("mark_as_read", SocketIoMessageMarkRequest.class, (client, data, ackSender) -> {
            Long userId = getUserIdFromClient(client);
            messageService.markMessageAsRead(data.roomId(), userId);
        });
    }

    private Long getUserIdFromClient(SocketIOClient client) {
        List<String> tokens = client.getHandshakeData().getUrlParams().get("token"); // 토큰 불러옴

        return jwtTokenProvider.getUserIdFromToken(tokens.get(0));
    }

    public ConnectListener listenConnected() {
        return (client) -> {
            try {
                String token = client.getHandshakeData().getUrlParams().get("token").get(0);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                log.info(":: SocketIo Connect - userId : {} ", userId);

                List<Long> joinedRoomIds = signalRoomRepository.findRoomIdsByUserId(userId);

                for(Long roomId: joinedRoomIds) {
                    String roomKey = "room-" + roomId;
                    client.joinRoom(roomKey);
                    log.info("# user {} → {}", userId, roomKey);
                }

                client.set("userId", userId);
            } catch (Exception e) {
                log.warn("⚠️ JWT 토큰 검증 실패 - 연결 거부: {}", e.getMessage());
                client.disconnect();
            }

        };
    }

    public DisconnectListener listenDisconnected() {
        return (client) -> {
            String sessionId = client.getSessionId().toString();
            log.info(":: SocketIo Disconnect - " + sessionId + " ::");

            Long userId = client.get("userId");

            for(String room : client.getAllRooms()) {
                client.leaveRoom(room);
                log.info("# user {} → {}", userId, room);
            }

            client.disconnect();
        };
    }
}
