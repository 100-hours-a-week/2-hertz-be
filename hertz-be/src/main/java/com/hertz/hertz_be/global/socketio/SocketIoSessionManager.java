package com.hertz.hertz_be.global.socketio;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketIoSessionManager {

    private final Map<Long, UUID> userIdToSessionIdMap = new ConcurrentHashMap<>();

    // 사용자가 연결될 때 호출
    public void registerSession(Long userId, UUID sessionId) {
        userIdToSessionIdMap.put(userId, sessionId);
    }

    // 연결 해제 시 호출
    public void unregisterSession(Long userId) {
        userIdToSessionIdMap.remove(userId);
    }

    // 현재 연결 여부 확인
    public boolean isConnected(Long userId) {
        return userIdToSessionIdMap.containsKey(userId);
    }

    public int getConnectedUserCount() {
        return userIdToSessionIdMap.size();
    }

    // 세션 ID로 사용자 ID 조회
    public Optional<Long> getUserIdBySessionId(UUID sessionId) {
        return userIdToSessionIdMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(sessionId))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}