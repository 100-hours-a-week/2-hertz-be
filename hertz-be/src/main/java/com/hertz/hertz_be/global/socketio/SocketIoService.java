package com.hertz.hertz_be.global.socketio;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
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
    public SignalMessage saveMessage(Long roomId, Long senderId, String plainText) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getHttpStatus(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getMessage()
                ));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));

        String encrypted = aesUtil.encrypt(plainText);

        SignalMessage message = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(sender)
                .message(encrypted)
                .build();

        return signalMessageRepository.save(message);
    }

    public SocketIoMessageResponse processAndRespond(Long roomId, Long senderId, String plainText) {
        SignalMessage saved = saveMessage(roomId, senderId, plainText);
        String decrypted = aesUtil.decrypt(saved.getMessage());
        return SocketIoMessageResponse.from(saved, decrypted);
    }

    @Transactional
    public void markMessageAsRead(Long roomId, Long userId) {
        signalMessageRepository.markUnreadMessagesAsRead(roomId, userId);
    }

}
