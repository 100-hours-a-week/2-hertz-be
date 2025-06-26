package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageMarkRequest;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageRequest;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.util.SocketIoTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SocketIoController {
    private final SocketIOServer server;
    private final SocketIoService messageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocketIoTokenUtil socketIoTokenUtil;
    private final SignalRoomRepository signalRoomRepository;
    private final Map<Long, UUID> connectedUsers = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        server.addConnectListener(onConnected());
        server.addEventListener("send_message", SocketIoMessageRequest.class, this::handleSendMessage);
        server.addEventListener("mark_as_read", SocketIoMessageMarkRequest.class, this::handleMarkAsRead);
        server.addDisconnectListener(onDisconnected());
    }

    private ConnectListener onConnected() {
        return client -> {
            try {
                String cookie = client.getHandshakeData().getHttpHeaders().get("cookie");
                log.info("🍪 쿠키: {}", cookie);

                String refreshToken = socketIoTokenUtil.extractCookie(cookie, "refreshToken");
                log.info("🔐 추출된 리프레시 토큰: {}", refreshToken);

                if (refreshToken == null) {
                    log.warn("❗ refreshToken 없음, 연결 종료");
                    client.disconnect();
                    return;
                }

                Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
                client.set("userId", userId);
                client.sendEvent("init_user", userId);

                List<Long> roomIds = signalRoomRepository.findRoomIdsByUserId(userId);
                for (Long roomId : roomIds) {
                    client.joinRoom("room-" + roomId);
                    log.info("🚀 userId={} → room-{} 참가", userId, roomId);
                }

                connectedUsers.put(userId, client.getSessionId());
                log.info("✅ userId [{}] 접속 , 현재 접속자 수={}", userId, getConnectedUserCount());
            } catch (Exception e) {
                log.error("❌ 연결 중 예외 발생: {}", e.getMessage(), e);
                client.disconnect();
            }

        };
//        return client -> {
//            String cookie = client.getHandshakeData().getHttpHeaders().get("cookie");
//            String refreshToken = socketIoTokenUtil.extractCookie(cookie, "refreshToken");
//
//            if (refreshToken == null) { // null만 체크
//                client.disconnect();
//                return;
//            }
//
//            try {
//                Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
//                client.set("userId", userId);
//                client.sendEvent("init_user", userId); // 클라이언트에게 userId 전달
//
//                // 사용자가 속한 채팅방 목록 조회 (예: DB에서)
//                List<Long> userRoomIds = signalRoomRepository.findRoomIdsByUserId(userId);
//
//                for (Long roomId : userRoomIds) {
//                    client.joinRoom("room-" + roomId);
//                    log.info("🚀 userId={} → room-{} 참가", userId, roomId);
//                }
//
//                connectedUsers.put(userId, client.getSessionId());
//                log.info("✅ userId [{}] 접속 , 현재 접속자 수={}", userId, getConnectedUserCount());
//            } catch (Exception e) {
//                log.error("Socket 연결 실패: 토큰 파싱 중 예외 발생. error={}", e.getMessage(), e);
//                client.disconnect();
//            }
//        };
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            Long userId = getUserIdFromClient(client);
            String sessionId = client.getSessionId().toString();
            log.warn("🔌 disconnect 발생! sessionId={}, userId={}", sessionId, userId);

            if (userId != null) {
                connectedUsers.remove(userId);
                log.info("❌ userId [{}] 연결 종료, 현재 접속자 수={}", userId, getConnectedUserCount());
            }
        };
    }

    // 메세지 수신
    private void handleSendMessage(SocketIOClient client, SocketIoMessageRequest data, AckRequest ackSender) {
        Long senderId = getUserIdFromClient(client);
        log.info("📨 메시지 수신: [{}] → 방 {}: {}", senderId, data.roomId(), data.message());

        // 저장 + 복호화 응답 생성
        SocketIoMessageResponse response = messageService.processAndRespond(data.roomId(), senderId, data.message());

        String roomKey = "room-" + data.roomId();
        server.getRoomOperations(roomKey).sendEvent("receive_message", response);
    }

    // 메세지 읽음 처리
    private void handleMarkAsRead(SocketIOClient client, SocketIoMessageMarkRequest data, AckRequest ackSender) {
        Long userId = getUserIdFromClient(client);
        messageService.markMessageAsRead(data.roomId(), userId);
    }



    public int getConnectedUserCount() {
        return connectedUsers.size();
    }

    private Long getUserIdFromClient(SocketIOClient client) {
        Object attr = client.get("userId");
        if (attr instanceof Long userId) {
            return userId;
        } else {
            log.warn("userId 속성이 존재하지 않거나 잘못된 타입: {}", attr);
            return null;
        }
    }

}
