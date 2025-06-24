package com.hertz.hertz_be.domain.channel.service.v3;

import com.hertz.hertz_be.domain.channel.dto.request.v3.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelSummaryDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service("channelServiceV3")
@RequiredArgsConstructor
public class ChannelService {
    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final AsyncChannelService asyncChannelService;
    private final AESUtil aesUtil;

    public ChannelListResponseDto getPersonalSignalRoomList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChannelRoomProjection> result = signalRoomRepository.findChannelRoomsWithPartnerAndLastMessage(userId, pageable);

        if (result.isEmpty()) {
            return null;
        }

        List<ChannelSummaryDto> list = result.getContent().stream()
                .filter(p -> {
                    boolean isSender = userId.equals(p.getSenderUserId());
                    LocalDateTime exitedAt = isSender ? p.getSenderExitedAt() : p.getReceiverExitedAt();
                    return exitedAt == null;
                })
                .map(p -> ChannelSummaryDto.fromProjectionWithDecrypt(p, aesUtil))
                .toList();

        return new ChannelListResponseDto(list, result.getNumber(), result.getSize(), result.isLast());
    }

    @Transactional
    public ChannelRoomResponseDto getChannelRoom(Long roomId, Long userId, int page, int size) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getHttpStatus(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getMessage()
                ));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getHttpStatus(),
                    "사용자가 참여 중인 채팅방 아닙니다."
            );
        }

        if (room.isUserExited(userId)) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getHttpStatus(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getMessage()
            );
        }

        boolean isPartnerExited = room.isPartnerExited(userId);

        Long partnerId = room.getPartnerUser(userId).getId();

        User partner = userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()
                ));

        Optional<RoomWithLastSenderProjection> result = signalMessageRepository.findRoomsWithLastSender(roomId);

        if(result.isPresent()){
            RoomWithLastSenderProjection lastSender = result.get();
            if (!Objects.equals(lastSender.getLastSenderId(), userId)) {
                signalMessageRepository.markAllMessagesAsReadByRoomId(roomId);
            }
        }

        asyncChannelService.notifyMatchingConvertedInChannelRoom(room, userId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendAt"));
        Page<SignalMessage> messagePage = signalMessageRepository.findBySignalRoom_Id(roomId, pageable);

        List<ChannelRoomResponseDto.MessageDto> messages = messagePage.getContent().stream()
                .map(msg -> ChannelRoomResponseDto.MessageDto.fromProjectionWithDecrypt(msg, aesUtil))
                .toList();

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncChannelService.updateNavbarMessageNotification(userId);
            }
        });

        return ChannelRoomResponseDto.of(roomId, partner, room.getRelationType(), isPartnerExited, String.valueOf(room.getCategory()), messages, messagePage);
    }

    @Transactional
    public SendSignalResponseDto sendSignal(Long senderUserId, SendSignalRequestDto dto) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        "시그널 보내기 요청한 사용자가 존재하지 않습니다."
                ));

        User receiver = userRepository.findById(dto.getReceiverUserId())
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()
                ));


        String userPairSignal = generateUserPairSignal(sender.getId(), receiver.getId());
        Optional<SignalRoom> existingRoom = signalRoomRepository.findByUserPairSignal(userPairSignal);
        if (existingRoom.isPresent()) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getCode(),
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getHttpStatus(),
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getMessage()
            );
        } else if (Objects.equals(sender.getId(), receiver.getId())) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getCode(),
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getHttpStatus(),
                    "자기 자신에게는 시그널을 보낼 수 없습니다."
            );
        }

        SignalRoom signalRoom = SignalRoom.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .category(dto.getCategory())
                .senderMatchingStatus(MatchingStatus.SIGNAL)
                .receiverMatchingStatus(MatchingStatus.SIGNAL)
                .userPairSignal(userPairSignal)
                .build();

        try {
            signalRoomRepository.save(signalRoom);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getCode(),
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getHttpStatus(),
                    ChannelResponseCode.ALREADY_IN_CONVERSATION.getMessage()
            );
        }

        String encryptMessage = aesUtil.encrypt(dto.getMessage());

        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(signalRoom)
                .senderUser(sender)
                .message(encryptMessage)
                .isRead(false)
                .build();
        signalMessageRepository.save(signalMessage);

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncChannelService.sendNewMessageNotifyToPartner(signalMessage, receiver.getId(), true);
            }
        });

        return new SendSignalResponseDto(signalRoom.getId());
    }

    public static String generateUserPairSignal(Long userId1, Long userId2) {
        Long min = Math.min(userId1, userId2);
        Long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }
}
