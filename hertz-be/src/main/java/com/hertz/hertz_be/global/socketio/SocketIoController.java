package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class SocketIoController {
    private final SocketIOServer server;
    private final SocketIoService messageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocketIoTokenUtil socketIoTokenUtil;
    private final SignalRoomRepository signalRoomRepository;
    private final UserRepository userRepository;
    private final SocketIoSessionManager socketIoSessionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        server.addConnectListener(onConnected());
        server.addEventListener("send_message", SocketIoMessageRequest.class, this::handleSendMessage);
        //server.addEventListener("mark_as_read", SocketIoMessageMarkRequest.class, this::handleMarkAsRead);
        server.addDisconnectListener(onDisconnected());
    }

    private ConnectListener onConnected() {
        return client -> {
            try {
                String cookie = client.getHandshakeData().getHttpHeaders().get("cookie");
                if (cookie == null) {
                    log.warn("[Connect Fail] 쿠키 없음 → 연결 종료: sessionId={}", client.getSessionId());
                    client.disconnect();
                    return;
                }

                String refreshToken = socketIoTokenUtil.extractCookie(cookie, "refreshToken");
                if (refreshToken == null || refreshToken.isBlank()) {
                    log.warn("[Connect Fail] refreshToken 추출 실패 → 연결 종료: sessionId={}", client.getSessionId());
                    client.disconnect();
                    return;
                }

                Long userId = null;
                try {
                    userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
                    if(!userRepository.existsById(userId)) {
                        log.warn("[Connect Fail] 존재하지 않는 userId={} → 연결 종료: sessionId={}", userId, client.getSessionId());
                        client.disconnect();
                        return;
                    }
                } catch (Exception jwtEx) {
                    log.error("[Connect Fail] JWT 토큰 파싱 실패: {}, 토큰={}", jwtEx.getMessage(), refreshToken);
                    client.disconnect();
                    return;
                }

                List<Long> roomIds = signalRoomRepository.findRoomIdsByUserId(userId);
                client.set("roomIds", roomIds); // 채팅방 목록 저장
                client.set("userId", userId);
                client.sendEvent("init_user", userId);

                socketIoSessionManager.registerClient(userId, client);
                log.info("[Connect Success] userId [{}] 접속 완료, 현재 접속자 수={}", userId, getConnectedUserCount());

            } catch (Exception e) {
                log.error("[Connect Fail] 연결 처리 중 예외 발생: {}", e.getMessage(), e);
                client.disconnect();
            }
        };
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            Long userId = getUserIdFromClient(client);
            @SuppressWarnings("unchecked")
            List<Long> roomIds = (List<Long>) client.get("roomIds");
            Long joinRoomId = client.get("joinRoomId");

            if(roomIds != null && roomIds.contains(joinRoomId)) {
                client.leaveRoom("room-" + joinRoomId);
            }

            if (userId != null) {
                socketIoSessionManager.unregisterClient(userId);
                log.info("[Disconnect Success] userId={}, 연결 종료, 현재 접속자 수={}", userId, getConnectedUserCount());
            }
        };
    }

    // 메세지 수신
    private void handleSendMessage(SocketIOClient client, SocketIoMessageRequest data, AckRequest ackSender) {
        Long senderId = getUserIdFromClient(client);
        Long roomId = data.roomId();

        @SuppressWarnings("unchecked")
        List<Long> roomIds = (List<Long>) client.get("roomIds");

        if(roomIds == null || !roomIds.contains(roomId)) {
            log.warn("[Invalid RoomID] [{}]는 [{}} roomID에 접근 권한 없음", senderId, roomId);
            return;
        } else {
            client.set("joinRoomId", roomId);
            client.joinRoom("room-" + roomId);
            log.info("[JoinRoom Success] userId={} → room-{} 참가 성공", client.get("userId"), roomId);
        }

        // 저장 + 복호화 응답 생성
        SocketIoMessageResponse response = messageService.processAndRespond(data.roomId(), senderId, data.message(), data.sendAt());
        server.getRoomOperations("room-" + data.roomId()).sendEvent("receive_message", response);
    }

    // 메세지 읽음 처리
    private void handleMarkAsRead(SocketIOClient client, SocketIoMessageMarkRequest data, AckRequest ackSender) {
        Long userId = getUserIdFromClient(client);
        Long roomId = data.roomId();

        log.info("✅ 읽음 처리 요청: userId={}, roomId={}", userId, roomId);

        // DB 읽음 처리 + afterCommit에서 웹소켓 응답 브로드캐스트
        messageService.markMessageAsRead(roomId, userId);
    }

    public int getConnectedUserCount() {
        return socketIoSessionManager.getConnectedUserCount();
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
