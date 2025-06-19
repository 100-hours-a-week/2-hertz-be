package com.hertz.hertz_be.domain.channel.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.hertz.hertz_be.domain.channel.dto.request.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.*;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.*;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
import com.hertz.hertz_be.domain.channel.entity.*;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDto;
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
@Service
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
    private final ChannelRoomRepository channelRoomRepository;
    private final InterestsService interestsService;
    private final AsyncChannelService asyncChannelService;
    private final WebClient webClient;
    private final AESUtil aesUtil;
    private SocketIOServer socketIOServer;

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
                          @Value("${ai.server.ip}") String aiServerIp, SocketIOServer socketIOServer) {
        this.userRepository = userRepository;
        this.tuningRepository = tuningRepository;
        this.tuningResultRepository = tuningResultRepository;
        this.userInterestsRepository = userInterestsRepository;
        this.signalMessageRepository = signalMessageRepository;
        this.signalRoomRepository = signalRoomRepository;
        this.channelRoomRepository = channelRoomRepository;
        this.interestsService = interestsService;
        this.asyncChannelService = asyncChannelService;
        this.aesUtil = aesUtil;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
        this.socketIOServer = socketIOServer;
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
    public boolean hasNewMessages(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));

        List<SignalRoom> allRooms = Stream.concat(
                user.getSentSignalRooms().stream(),
                user.getReceivedSignalRooms().stream()
        ).collect(Collectors.toList());

        if (allRooms.isEmpty()) return false;

        return signalMessageRepository.existsBySignalRoomInAndSenderUserNotAndIsReadFalse(allRooms, user);

    }

    public Map<String, List<String>> getUserInterests(Long userId) {
        Map<String, List<String>> interestsMap = new LinkedHashMap<>();

        userInterestsRepository.findByUserId(userId).stream()
                .filter(ui -> ui.getCategoryItem().getCategory().getCategoryType() == InterestsCategoryType.INTEREST)
                .forEach(ui -> {
                    String categoryName = ui.getCategoryItem().getCategory().getName();
                    String itemName = ui.getCategoryItem().getName();
                    interestsMap.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(itemName);
                });

        return interestsMap;
    }

    public Map<String, List<String>> extractSameInterests(Map<String, List<String>> interests1, Map<String, List<String>> interests2) {
        Map<String, List<String>> sameInterests = new LinkedHashMap<>();

        for (String category : interests1.keySet()) {
            List<String> list1 = interests1.getOrDefault(category, Collections.emptyList());
            List<String> list2 = interests2.getOrDefault(category, Collections.emptyList());

            Set<String> common = new HashSet<>(list1);
            common.retainAll(list2);

            if (!common.isEmpty()) {
                sameInterests.put(category, List.of(common.iterator().next()));
            } else {
                sameInterests.put(category, Collections.emptyList());
            }
        }

        return sameInterests;

    }

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

        return ChannelRoomResponseDto.of(roomId, partner, room.getRelationType(), isPartnerExited, messages, messagePage);
    }

    @Transactional
    public void sendChannelMessage(Long roomId, Long userId, SendSignalRequestDto response) {
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

        // 2. 메세지 WebSocket 전송
        String roomKey = "room-" + roomId;
        socketIOServer.getRoomOperations(roomKey)
                        .sendEvent("receive_message", SocketIoMessageResponse.from(signalMessage));

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncChannelService.notifyMatchingConverted(room);
                asyncChannelService.sendNewMessageNotifyToPartner(signalMessage, partnerId, false);
            }
        });
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


        /**
         * 매칭 수락/거절 후 현재 관계 반환
         */
        if(matchingStatus == MatchingStatus.MATCHED) {
            return signalRoomRepository.findMatchResultByUser(userId, room.getId());
        } else {
            return ChannelResponseCode.MATCH_REJECTION_SUCCESS.getCode();
        }
    }


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
}
