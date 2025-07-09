package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageMarkResponse;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class SocketIoService {

    @PersistenceContext
    private EntityManager entityManager;

    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final UserRepository userRepository;
    private final AESUtil aesUtil;
    private final SocketIOServer server;
    private final AsyncChannelService asyncChannelService;

    @Transactional
    public SignalMessage saveMessage(Long roomId, Long senderId, String plainText, LocalDateTime sendAt) {
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

        if (!room.isParticipant(senderId)) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getHttpStatus(),
                    "사용자가 참여 중인 채팅방 아닙니다."
            );
        }

        Long partnerId = room.getPartnerUser(senderId).getId();
        userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()
                ));

        String encrypted = aesUtil.encrypt(plainText);
        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(sender)
                .message(encrypted)
                .sendAt(sendAt)
                .build();

        return signalMessageRepository.save(signalMessage);

//        signalMessageRepository.save(signalMessage);
//        entityManager.flush();
//
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                asyncChannelService.notifyMatchingConverted(room);
//                asyncChannelService.sendNewMessageNotifyToPartner(signalMessage, partnerId, false);
//            }
//        });
//
//        return signalMessage;
    }

    public SocketIoMessageResponse processAndRespond(Long roomId, Long senderId, String plainText, LocalDateTime sendAt) {
        SignalMessage saved = saveMessage(roomId, senderId, plainText, sendAt);
        String decrypted = aesUtil.decrypt(saved.getMessage());
        return SocketIoMessageResponse.from(saved, decrypted);
    }

    @Transactional
    public void markMessageAsRead(Long roomId, Long userId) {
        signalMessageRepository.markUnreadMessagesAsRead(roomId, userId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                SocketIoMessageMarkResponse response = new SocketIoMessageMarkResponse(roomId, userId, LocalDateTime.now());
                server.getRoomOperations("room-" + roomId)
                        .sendEvent("mark_as_read", response);

                log.info("📡 읽음 상태 전송 완료: {}", response);
            }
        });
    }
}
