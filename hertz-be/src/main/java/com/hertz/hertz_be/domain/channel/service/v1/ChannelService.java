package com.hertz.hertz_be.domain.channel.service.v1;

import com.hertz.hertz_be.domain.channel.dto.request.v3.SendMessageRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.*;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.channel.service.SseChannelService;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.*;
import com.hertz.hertz_be.domain.channel.entity.*;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.v1.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.responsecode.*;
import com.hertz.hertz_be.domain.channel.repository.*;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service("channelServiceV1")
@RequiredArgsConstructor
public class ChannelService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final TuningRepository tuningRepository;
    private final TuningResultRepository tuningResultRepository;
    private final UserInterestsRepository userInterestsRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final InterestsService interestsService;
    private final AsyncChannelService asyncChannelService;
    private final WebClient webClient;
    private final AESUtil aesUtil;

    @Value("${channel.message.page.size}")
    private int channelMessagePageSize;

    @Autowired
    public ChannelService(UserRepository userRepository,
                          TuningRepository tuningRepository,
                          TuningResultRepository tuningResultRepository,
                          UserInterestsRepository userInterestsRepository,
                          SignalRoomRepository signalRoomRepository,
                          SignalMessageRepository signalMessageRepository,
                          ChannelRoomRepository channelRoomRepository,
                          InterestsService interestsService,
                          AsyncChannelService asyncChannelService,
                          SseChannelService matchingStatusScheduler,
                          AESUtil aesUtil,
                          @Value("${ai.server.ip}") String aiServerIp) {
        this.userRepository = userRepository;
        this.tuningRepository = tuningRepository;
        this.tuningResultRepository = tuningResultRepository;
        this.userInterestsRepository = userInterestsRepository;
        this.signalMessageRepository = signalMessageRepository;
        this.signalRoomRepository = signalRoomRepository;
        this.interestsService = interestsService;
        this.asyncChannelService = asyncChannelService;
        this.aesUtil = aesUtil;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
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
                .category(Category.FRIEND)
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

    public static String generateUserPairSignal(Long userId1, Long userId2) {
        Long min = Math.min(userId1, userId2);
        Long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));
    }

    public boolean hasSelectedInterests(User user) {
        return userInterestsRepository.existsByUser(user);
    }

    @Transactional
    public TuningResponseDto getTunedUser(Long userId) {
        User requester = getUserById(userId);

        if (!hasSelectedInterests(requester)) {
            throw new BusinessException(
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getCode(),
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getHttpStatus(),
                    ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getMessage()
            );
        }

        Tuning tuning = getOrCreateTuning(requester);

        if (!tuningResultRepository.existsByTuning(tuning)) {
            boolean saved = fetchAndSaveTuningResultsFromAiServer(userId, tuning);
            if (!saved) return null;
        }

        Optional<TuningResult> optionalResult = tuningResultRepository.findFirstByTuningOrderByLineupAsc(tuning);
        if (optionalResult.isEmpty()) {
            boolean saved = fetchAndSaveTuningResultsFromAiServer(userId, tuning);
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


    private boolean fetchAndSaveTuningResultsFromAiServer(Long userId, Tuning tuning) {
        Map<String, Object> responseMap = requestTuningFromAiServer(userId);
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

            saveTuningResults(userIdList, tuning);
            return true;

        } else {
            throw new BusinessException(
                    NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                    NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                    "튜닝 과정에서 BE 서버 오류 발생했습니다."
            );
        }
    }


    private Map<String, Object> requestTuningFromAiServer(Long userId) {
        String uri = "/api/v1/tuning?userId=" + userId;
        Map<String, Object> responseMap = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (responseMap == null || !responseMap.containsKey("code")) {
            throw new BusinessException(
                    NewResponseCode.AI_SERVER_ERROR.getCode(),
                    NewResponseCode.AI_SERVER_ERROR.getHttpStatus(),
                    "튜닝 과정에서 AI 서버 오류 발생했습니다."
            );
        }
        return responseMap;
    }

    private Tuning getOrCreateTuning(User user) {
        return tuningRepository.findByUserAndCategory(user, Category.FRIEND)
                .orElseGet(() -> tuningRepository.save(
                        Tuning.builder()
                                .user(user)
                                .category(Category.FRIEND)
                                .build()));
    }

    private void saveTuningResults(List<Integer> userIdList, Tuning tuning) {
        int lineup = 1;
        User requester = tuning.getUser();

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

            boolean alreadyExists = signalRoomRepository.existsBySenderUserAndReceiverUser(requester, matchedUser)
                    || signalRoomRepository.existsBySenderUserAndReceiverUser(matchedUser, requester);

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
    public ChannelListResponseDto getPersonalSignalRoomList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SignalRoom> signalRooms = signalRoomRepository.findAllOrderByLastMessageTimeDesc(userId, pageable);

        if (signalRooms.isEmpty()) {
            return new ChannelListResponseDto(List.of(), page, size, true);
        }

        List<ChannelSummaryDto> list = signalRooms.getContent().stream()
                .filter(room -> !room.isUserExited(userId)) // 나간 방 제외
                .map(room -> toChannelSummaryDtoV1(room, userId))
                .toList();

        return new ChannelListResponseDto(list, signalRooms.getNumber(), signalRooms.getSize(), signalRooms.isLast());
    }

    private ChannelSummaryDto toChannelSummaryDtoV1(SignalRoom room, Long userId) {
        User partner = room.getPartnerUser(userId);
        SignalMessage lastMessage = extractLastMessage(room);

        String decryptedMessage = decryptMessageSafe(lastMessage != null ? lastMessage.getMessage() : null);
        LocalDateTime lastMessageTime = lastMessage != null ? lastMessage.getSendAt() : null;
        boolean isRead = isMessageReadByUser(lastMessage, userId);

        return new ChannelSummaryDto(
                room.getId(),
                partner.getProfileImageUrl(),
                partner.getNickname(),
                decryptedMessage,
                lastMessageTime,
                isRead,
                room.getRelationType()
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

        asyncChannelService.notifyMatchingConvertedInChannelRoom(room, userId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendAt"));
        Page<SignalMessage> messagePage = signalMessageRepository.findBySignalRoom_Id(roomId, pageable);

        List<ChannelRoomResponseDto.MessageDto> messages = messagePage.getContent().stream()
                .map(msg -> ChannelRoomResponseDto.MessageDto.fromProjectionWithDecrypt(msg, aesUtil))
                .toList();

        entityManager.flush();
        registerAfterCommitCallback(() -> {
            asyncChannelService.updateNavbarMessageNotification(userId);
        });

        return ChannelRoomResponseDto.of(roomId, partner, room.getRelationType(), isPartnerExited, messages, messagePage);
    }

    @Transactional
    public void sendChannelMessage(Long roomId, Long userId, SendMessageRequestDto response) {
        // 1. 메세지 DB 저장
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getHttpStatus(),
                        ChannelResponseCode.CHANNEL_NOT_FOUND.getMessage()
                ));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        "메세지 전송을 요청한 사용자가 존재하지 않습니다."
                ));

        if (!room.isParticipant(userId)) {
            throw new BusinessException(
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(),
                    ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getHttpStatus(),
                    "사용자가 참여 중인 채팅방 아닙니다."
            );
        }

        Long partnerId = room.getPartnerUser(userId).getId();
        userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()
                ));

        String encryptMessage = aesUtil.encrypt(response.getMessage());

        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(user)
                .message(encryptMessage)
                .build();

        signalMessageRepository.save(signalMessage);
        entityManager.flush();
        registerAfterCommitCallback(() -> {
            asyncChannelService.notifyMatchingConverted(room);
            asyncChannelService.sendNewMessageNotifyToPartner(room, signalMessage, partnerId, false);
        });

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

