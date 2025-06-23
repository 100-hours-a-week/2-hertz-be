package com.hertz.hertz_be.global.socketio;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelNotFoundException;
import com.hertz.hertz_be.domain.channel.responsecode.ForbiddenChannelException;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.responsecode.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.util.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocketIoService {

    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final UserRepository userRepository;
    private final AESUtil aesUtil;

    @Transactional
    public SignalMessage saveMessage(Long roomId, Long userId, String plainText) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(ChannelNotFoundException::new);

        if (!room.isParticipant(userId)) {
            throw new ForbiddenChannelException();
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND, "사용자 없음"));

        String encrypted = aesUtil.encrypt(plainText);

        SignalMessage message = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(sender)
                .message(encrypted)
                .build();

        return signalMessageRepository.save(message);
    }

    @Transactional
    public void markMessageAsRead(Long roomId, Long userId) {
        signalMessageRepository.markUnreadMessagesAsRead(roomId, userId);
    }

}
