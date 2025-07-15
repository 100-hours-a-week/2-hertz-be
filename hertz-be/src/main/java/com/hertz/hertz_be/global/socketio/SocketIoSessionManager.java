package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketIoSessionManager {

    // userId → SocketIOClient
    private final Map<Long, SocketIOClient> userClientMap = new ConcurrentHashMap<>();

    // 사용자가 연결될 때 호출
    public void registerClient(Long userId, SocketIOClient client) {
        userClientMap.put(userId, client);
    }

    // 연결 해제 시 호출
    public void unregisterClient(Long userId) {
        userClientMap.remove(userId);
    }

    // 연결 여부 확인
    public boolean isConnected(Long userId) {
        return userClientMap.containsKey(userId);
    }

    // 해당 user가 특정 room에 접속 중인지 확인
    public boolean isUserInRoom(Long userId, String roomKey) {
        SocketIOClient client = userClientMap.get(userId);
        if (client == null) return false;
        return client.getAllRooms().contains(roomKey);
    }

    public int getConnectedUserCount() {
        return userClientMap.size();
    }

    // (선택) getClient() 메서드도 추가 가능
    public SocketIOClient getClient(Long userId) {
        return userClientMap.get(userId);
    }
}