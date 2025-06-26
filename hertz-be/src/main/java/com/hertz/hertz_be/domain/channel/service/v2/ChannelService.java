package com.hertz.hertz_be.domain.channel.service.v2;

import com.hertz.hertz_be.domain.channel.dto.request.v2.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.*;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service("channelServiceV2")
@RequiredArgsConstructor
public class ChannelService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final AsyncChannelService asyncChannelService;

    @Transactional
    public void leaveChannelRoom(Long roomId, Long userId) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getHttpStatus(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getMessage()
                ));

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(
                    UserResponseCode.USER_NOT_FOUND.getCode(),
                    UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                    "채팅방 나가기 요청한 사용자가 존재하지 않습니다."
            );
        }

        room.leaveChannelRoom(userId);
    }

    @Transactional
    public String channelMatchingStatusUpdate(Long userId, SignalMatchingRequestDto response, MatchingStatus matchingStatus) {
        SignalRoom room = signalRoomRepository.findById(response.getChannelRoomId())
                .orElseThrow(() -> new BusinessException(
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getHttpStatus(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getMessage()
                ));

        int updatedSender = signalRoomRepository.updateSenderMatchingStatus(room.getId(), userId, matchingStatus);
        int updatedReceiver = signalRoomRepository.updateReceiverMatchingStatus(room.getId(), userId, matchingStatus);

        if (updatedSender == 0 && updatedReceiver == 0) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getHttpStatus(),
                    "사용자가 참여 중인 채팅방 아닙니다."
            );
        }

        entityManager.flush();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncChannelService.notifyMatchingResultToPartner(room, userId, matchingStatus);
                asyncChannelService.createMatchingAlarm(room, userId);
            }
        });

        if(matchingStatus == MatchingStatus.MATCHED) {
            return signalRoomRepository.findMatchResultByUser(userId, room.getId());
        } else {
            return ChannelResponseCode.MATCH_REJECTION_SUCCESS.getCode();
        }
    }
}
