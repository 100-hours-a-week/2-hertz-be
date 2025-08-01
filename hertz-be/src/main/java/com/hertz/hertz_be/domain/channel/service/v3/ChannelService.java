package com.hertz.hertz_be.domain.channel.service.v3;

import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.channel.dto.request.v3.ChatReportRequestDto;
import com.hertz.hertz_be.domain.channel.dto.request.v3.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.*;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.TuningResult;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningResultRepository;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.infra.ai.client.TuningAiClient;
import com.hertz.hertz_be.global.infra.ai.dto.response.AiChatReportResponseDto;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service("channelServiceV3")
@RequiredArgsConstructor
public class ChannelService {
    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final TuningResultRepository tuningResultRepository;
    private final TuningRepository tuningRepository;
    private final InterestsService interestsService;
    private final UserInterestsRepository userInterestsRepository;
    private final AsyncChannelService asyncChannelService;
    private final AlarmService alarmService;
    private final AESUtil aesUtil;
    private final TuningAiClient tuningAiClient;

    @Value("${channel.message.page.size}")
    private int channelMessagePageSize;

    @Transactional(readOnly = true)
    public ChannelListResponseDto getPersonalSignalRoomList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SignalRoom> signalRooms = signalRoomRepository.findAllOrderByLastMessageTimeWithUsers(userId, pageable);


        if (signalRooms.isEmpty()) {
            return new ChannelListResponseDto(List.of(), page, size, true);
        }

        List<ChannelSummaryDto> list = signalRooms.getContent().stream()
                .filter(room -> !room.isUserExited(userId))
                .map(room -> toChannelSummaryDto(room, userId))
                .toList();

        return new ChannelListResponseDto(list, signalRooms.getNumber(), signalRooms.getSize(), signalRooms.isLast());
    }

    private ChannelSummaryDto toChannelSummaryDto(SignalRoom room, Long userId) {
        User partner = room.getPartnerUser(userId);
        SignalMessage lastMessage = extractLastMessage(room);

        String decryptedMessage = decryptMessageSafe(lastMessage != null ? lastMessage.getMessage() : null);
        LocalDateTime lastMessageTime = lastMessage != null ? lastMessage.getSendAt() : null;
        boolean isRead = isMessageReadByUser(lastMessage, userId);

        // 특정 체팅방의 마지막 페이지 번호 계산
        int lastPageNumber = (int) Math.ceil((room.getMessages().size() * 1.0) / channelMessagePageSize) - 1;

        return new ChannelSummaryDto(
                room.getId(),
                partner.getProfileImageUrl(),
                partner.getNickname(),
                decryptedMessage,
                lastMessageTime,
                isRead,
                room.getCategory().name(),
                room.getRelationType(),
                lastPageNumber
        );
    }

    private SignalMessage extractLastMessage(SignalRoom room) {
        return room.getMessages().stream()
                .max(Comparator.comparing(SignalMessage::getSendAt))
                .orElse(null);
    }

    private String decryptMessageSafe(String encrypted) {
        if (encrypted == null) return "";
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception e) {
            return "메세지를 표시할 수 없습니다.";
        }
    }

    private boolean isMessageReadByUser(SignalMessage message, Long userId) {
        if (message == null) return true;
        return message.getSenderUser().getId().equals(userId) || message.getIsRead();
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

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendAt"));
        Page<SignalMessage> messagePage = signalMessageRepository.findBySignalRoom_Id(roomId, pageable);

        List<ChannelRoomResponseDto.MessageDto> messages = messagePage.getContent().stream()
                .map(msg -> ChannelRoomResponseDto.MessageDto.fromProjectionWithDecrypt(msg, aesUtil))
                .toList();

        entityManager.flush();
        registerAfterCommitCallback(() -> {
            asyncChannelService.updateNavbarMessageNotification(userId);
            asyncChannelService.notifyMatchingConvertedInChannelRoom(room, userId);
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

        if (!receiver.isCategoryAllowed(dto.getCategory())) {
            throw new BusinessException(
                    UserResponseCode.CATEGORY_IS_REJECTED.getCode(),
                    UserResponseCode.CATEGORY_IS_REJECTED.getHttpStatus(),
                    UserResponseCode.CATEGORY_IS_REJECTED.getMessage()
            );
        }

        boolean alreadyExists = signalRoomRepository.existsByUserPairAndCategory(sender, receiver, dto.getCategory());
        if (alreadyExists) {
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

        String userPairSignal = generateUserPairSignal(sender.getId(), receiver.getId(), dto.getCategory());

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
        registerAfterCommitCallback(() -> {
            asyncChannelService.sendNewMessageNotifyToPartner(signalRoom, signalMessage, receiver.getId(), true);
        });

        return new SendSignalResponseDto(signalRoom.getId());
    }

    public static String generateUserPairSignal(Long userId1, Long userId2, Category category) {
        Long min = Math.min(userId1, userId2);
        Long max = Math.max(userId1, userId2);
        return min + "_" + max + "_" + category;
    }

    @Transactional
    public TuningResponseDto getTunedUser(Long userId, String category) {
        User requester = getUserById(userId);

        if (!hasSelectedInterests(requester)) {
            throw new BusinessException(
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getCode(),
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getHttpStatus(),
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getMessage()
            );
        }

        Tuning tuning = getOrCreateTuning(requester, category);

        if (!tuningResultRepository.existsByTuning(tuning)) {
            boolean saved = fetchAndSaveTuningResultsFromAiServer(userId, tuning, category);
            if (!saved) return null;
        }

        Optional<TuningResult> optionalResult = tuningResultRepository.findFirstByTuningOrderByLineupAsc(tuning);
        if (optionalResult.isEmpty()) {
            boolean saved = fetchAndSaveTuningResultsFromAiServer(userId, tuning, category);
            if (!saved) return null;

            optionalResult = tuningResultRepository.findFirstByTuningOrderByLineupAsc(tuning);
            if (optionalResult.isEmpty()) return null;
        }

        TuningResult topResult = optionalResult.get();
        tuningResultRepository.delete(topResult);

        User matchedUser = topResult.getMatchedUser();
        if (matchedUser == null || matchedUser.getId() == null) return null;

        return buildTuningResponseDTO(userId, matchedUser);
    }

    private boolean fetchAndSaveTuningResultsFromAiServer(Long userId, Tuning tuning, String category) {
        Map<String, Object> responseMap = tuningAiClient.requestTuningByCategory(userId, category);
        String code = (String) responseMap.get("code");

        if (ChannelResponseCode.TUNING_SUCCESS_BUT_NO_MATCH.getCode().equals(code)) {
            return false;

        } else if (ChannelResponseCode.TUNING_BAD_REQUEST.getCode().equals(code)) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "AI 서버에서 bad request 발생했습니다."
            );

        } else if (ChannelResponseCode.TUNING_NOT_FOUND_USER.getCode().equals(code)) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "AI 서버에서 사용자를 찾을 수 없습니다."
            );

        } else if (ChannelResponseCode.TUNING_INTERNAL_SERVER_ERROR.getCode().equals(code)) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "튜닝 과정에서 AI 서버 오류 발생했습니다."
            );

        } else if (ChannelResponseCode.TUNING_SUCCESS.getCode().equals(code)) {
            Object dataObj = responseMap.get("data");
            if (!(dataObj instanceof Map)) {
                throw new BusinessException(
                        NewResponseCode.AI_SERVER_ERROR.getCode(),
                        NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                        "튜닝 과정에서 AI 서버 오류 발생했습니다."
                );
            }

            Map<?, ?> data = (Map<?, ?>) dataObj;
            List<Integer> userIdList = (List<Integer>) data.get("userIdList");
            if (userIdList == null || userIdList.isEmpty()) {
                throw new BusinessException(
                        NewResponseCode.AI_SERVER_ERROR.getCode(),
                        NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                        "튜닝 과정에서 AI 서버 오류 발생했습니다."
                );
            }

            saveTuningResults(userIdList, tuning, category);
            return true;

        } else {
            throw new BusinessException(
                    NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                    NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                    "튜닝 과정에서 BE 서버 오류 발생했습니다."
            );
        }
    }

    private Tuning getOrCreateTuning(User user, String category) {
        Category enumCategory = convertToCategory(category);

        return tuningRepository.findByUserAndCategory(user, enumCategory)
                .orElseGet(() -> tuningRepository.save(
                        Tuning.builder()
                                .user(user)
                                .category(enumCategory)
                                .build()));
    }

    private Category convertToCategory(String category) {
        return switch (category.toLowerCase()) {
            case "friend" -> Category.FRIEND;
            case "couple" -> Category.COUPLE;
            default -> throw new BusinessException(
                    NewResponseCode.BAD_REQUEST.getCode(),
                    NewResponseCode.BAD_REQUEST.getHttpStatus(),
                    "유효하지 않은 category: " + category
            );
        };
    }

    private void saveTuningResults(List<Integer> userIdList, Tuning tuning, String category) {
        int lineup = 1;
        User requester = tuning.getUser();
        Category enumCategory = convertToCategory(category);

        for (Integer matchedUserId : userIdList) {
            Long matchedId = Long.valueOf(matchedUserId);

            User matchedUser = userRepository.findById(matchedId)
                    .orElseThrow(() -> new BusinessException(
                            UserResponseCode.USER_DEACTIVATED.getCode(),
                            UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                            "BE 서버에 없는 사용자 id가 AI 서버로부터 넘어왔습니다."
                    ));

            if (!hasSelectedInterests(matchedUser)) {
                continue;
            }
            if (!matchedUser.isCategoryAllowed(enumCategory)) {
                continue;
            }
            boolean alreadyExists = signalRoomRepository.existsByUserPairAndCategory(requester, matchedUser, enumCategory);
            if (alreadyExists) continue;

            tuningResultRepository.save(
                    TuningResult.builder()
                            .tuning(tuning)
                            .matchedUser(matchedUser)
                            .lineup(lineup++)
                            .build()
            );
        }
    }

    private TuningResponseDto buildTuningResponseDTO(Long requesterId, User target) {
        Map<String, String> keywords = interestsService.getUserKeywords(target.getId());
        Map<String, List<String>> requesterInterests = interestsService.getUserInterests(requesterId);
        Map<String, List<String>> targetInterests = interestsService.getUserInterests(target.getId());
        Map<String, List<String>> sameInterests = interestsService.extractSameInterests(requesterInterests, targetInterests);

        return new TuningResponseDto(
                target.getId(),
                target.getProfileImageUrl(),
                target.getNickname(),
                target.getGender(),
                target.getOneLineIntroduction(),
                keywords,
                sameInterests
        );
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));
    }

    @Transactional(readOnly = true)
    public boolean hasSelectedInterests(User user) {
        return userInterestsRepository.existsByUser(user);
    }

    @Transactional
    public void reportMessage(Long reportSenderId, ChatReportRequestDto requestDto) {
        if (!userRepository.existsById(reportSenderId)) {
            throw new BusinessException(
                    UserResponseCode.USER_NOT_FOUND.getCode(),
                    UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                    "메세지 신고를 요청한 사용자가 존재하지 않습니다."
            );
        }

        if (!userRepository.existsById(requestDto.getReportedUserId())) {
            throw new BusinessException(
                    UserResponseCode.USER_DEACTIVATED.getCode(),
                    UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                    UserResponseCode.USER_DEACTIVATED.getMessage()
            );
        }

        sendReportedMessageToAi(requestDto);
    }

    private void sendReportedMessageToAi(ChatReportRequestDto requestDto) {
        boolean isMessageToxic = handleChatReportResult(requestDto);
        if (isMessageToxic) {
            alarmService.createAlertAlarm(requestDto.getReportedUserId(), requestDto.getMessageContent());
        }
    }

    private boolean handleChatReportResult(ChatReportRequestDto requestDto) {
        AiChatReportResponseDto response = tuningAiClient.sendChatReport(requestDto);
        String code = response.code();

        if (ChannelResponseCode.CENSORED_SUCCESS.name().equals(code)) {
            Map<String, Object> data = response.data();
            Object result = data != null ? data.get("result") : null;
            if (result instanceof Boolean booleanResult) {
                return booleanResult;
            }
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "메세지 신고 요청 과정에서 AI 응답 결과 포맷 오류"
            );

        } else if (ChannelResponseCode.CENSORED_BAD_REQUEST.name().equals(code)) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "메세지 신고 요청 과정에서 AI 서버에 잘못된 요청이 전달되었습니다."
            );

        } else if (ChannelResponseCode.CENSORED_INTERNAL_SERVER_ERROR.name().equals(code)) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "메세지 신고 요청 과정에서 AI 서버 오류 발생했습니다."
            );

        } else {
            throw new BusinessException(
                    NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                    NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                    "메세지 신고 요청 과정에서 예상치못한 AI 서버 응답을 받았습니다."
            );
        }
    }

    protected void registerAfterCommitCallback(Runnable callback) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    callback.run();
                }
            });
        } else {
            log.debug("⚠️ 트랜잭션 비활성 상태: 콜백 즉시 실행");
            callback.run();
        }
    }

}
