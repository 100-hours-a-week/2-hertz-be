package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.request.SignalMatchingRequestDTO;
import com.hertz.hertz_be.domain.channel.dto.response.*;
import com.hertz.hertz_be.domain.channel.entity.*;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.exception.*;
import com.hertz.hertz_be.domain.channel.repository.*;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.util.AESUtil;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.exception.AiServerBadRequestException;
import com.hertz.hertz_be.global.exception.AiServerErrorException;
import com.hertz.hertz_be.global.exception.AiServerNotFoundException;
import com.hertz.hertz_be.global.exception.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final UserRepository userRepository;
    private final TuningRepository tuningRepository;
    private final TuningResultRepository tuningResultRepository;
    private final UserInterestsRepository userInterestsRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final AsyncChannelService asyncChannelService;
    private final WebClient webClient;
    private final AESUtil aesUtil;

    @Autowired
    public ChannelService(UserRepository userRepository,
                          TuningRepository tuningRepository,
                          TuningResultRepository tuningResultRepository,
                          UserInterestsRepository userInterestsRepository,
                          SignalRoomRepository signalRoomRepository,
                          SignalMessageRepository signalMessageRepository,
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
        this.asyncChannelService = asyncChannelService;
        this.aesUtil = aesUtil;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
    }

    @Transactional
    public SendSignalResponseDTO sendSignal(Long senderUserId, SendSignalRequestDTO dto) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(UserNotFoundException::new);

        User receiver = userRepository.findById(dto.getReceiverUserId())
                .orElseThrow(UserWithdrawnException::new);


        String userPairSignal = generateUserPairSignal(sender.getId(), receiver.getId());
        Optional<SignalRoom> existingRoom = signalRoomRepository.findByUserPairSignal(userPairSignal);
        if (existingRoom.isPresent()) {
            throw new AlreadyInConversationException();
        } else if (Objects.equals(sender.getId(), receiver.getId())) {
            throw new CannotSendSignalToSelfException();
        }

        SignalRoom signalRoom = SignalRoom.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .category(Category.FRIEND)
                .senderMatchingStatus(MatchingStatus.SIGNAL)
                .receiverMatchingStatus(MatchingStatus.SIGNAL)
                .userPairSignal(userPairSignal)
                .build();
        signalRoomRepository.save(signalRoom);

        String encryptMessage = aesUtil.encrypt(dto.getMessage());

        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(signalRoom)
                .senderUser(sender)
                .message(encryptMessage)
                .isRead(false)
                .build();
        signalMessageRepository.save(signalMessage);

        return new SendSignalResponseDTO(signalRoom.getId());
    }

    public static String generateUserPairSignal(Long userId1, Long userId2) {
        Long min = Math.min(userId1, userId2);
        Long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    public boolean hasSelectedInterests(User user) {
        return userInterestsRepository.existsByUser(user);
    }

    @Transactional
    public TuningResponseDTO getTunedUser(Long userId) {
        User requester = getUserById(userId);

        if (!hasSelectedInterests(requester)) {
            throw new InterestsNotSelectedException();
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

        switch (code) {
            case ResponseCode.TUNING_SUCCESS_BUT_NO_MATCH -> {
                return false;
            }
            case ResponseCode.TUNING_BAD_REQUEST -> {
                throw new AiServerBadRequestException();
            }
            case ResponseCode.TUNING_NOT_FOUND_USER -> {
                throw new AiServerNotFoundException();
            }
            case ResponseCode.TUNING_INTERNAL_SERVER_ERROR -> {
                throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
            }
            case ResponseCode.TUNING_SUCCESS -> {
                Object dataObj = responseMap.get("data");
                if (!(dataObj instanceof Map data)) {
                    throw new AiServerErrorException(ResponseCode.TUNING_NOT_FOUND_DATA);
                }

                List<Integer> userIdList = (List<Integer>) data.get("userIdList");
                if (userIdList == null || userIdList.isEmpty()) {
                    throw new AiServerErrorException(ResponseCode.TUNING_NOT_FOUND_LIST);
                }

                saveTuningResults(userIdList, tuning);
                return true;
            }
            default -> throw new InternalServerErrorException();
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
            throw new AiServerErrorException(ResponseCode.TUNING_INTERNAL_SERVER_ERROR);
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
                    .orElseThrow(UserWithdrawnException::new);

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

    private TuningResponseDTO buildTuningResponseDTO(Long requesterId, User target) {
        Map<String, String> keywords = getUserKeywords(target.getId());

        Map<String, List<String>> requesterInterests = getUserInterests(requesterId);
        Map<String, List<String>> targetInterests = getUserInterests(target.getId());
        Map<String, List<String>> sameInterests = extractSameInterests(requesterInterests, targetInterests);

        return new TuningResponseDTO(
                target.getId(),
                target.getProfileImageUrl(),
                target.getNickname(),
                target.getGender(),
                target.getOneLineIntroduction(),
                keywords,
                sameInterests
        );
    }


    public Map<String, String> getUserKeywords(Long userId) {
        Map<String, String> keywords = userInterestsRepository.findByUserId(userId).stream()
                .filter(ui -> ui.getCategoryItem().getCategory().getCategoryType() == InterestsCategoryType.KEYWORD)
                .collect(
                        LinkedHashMap::new,
                        (map, ui) -> map.put(ui.getCategoryItem().getCategory().getName(), ui.getCategoryItem().getName()),
                        LinkedHashMap::putAll
                );

        return keywords;
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

            // 교집합 추출
            Set<String> common = new HashSet<>(list1);
            common.retainAll(list2);

            // 값이 있으면 1개만 반환, 없으면 빈 리스트
            if (!common.isEmpty()) {
                sameInterests.put(category, List.of(common.iterator().next()));
            } else {
                sameInterests.put(category, Collections.emptyList());
            }
        }

        return sameInterests;
    }

    @Transactional(readOnly = true)
    public boolean hasNewMessages(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        List<SignalRoom> allRooms = Stream.concat(
                user.getSentSignalRooms().stream(),
                user.getReceivedSignalRooms().stream()
        ).collect(Collectors.toList());

        if (allRooms.isEmpty()) return false;

        return signalMessageRepository.existsBySignalRoomInAndSenderUserNotAndIsReadFalse(allRooms, user);
    }

    // Todo: 추후 시그널 -> 채널로 마이그레이션 시 메소드명 변경 필요 (getPersonalSignalRoomList -> getPersonalChannelList)
    public ChannelListResponseDto getPersonalSignalRoomList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChannelRoomProjection> result = signalRoomRepository.findChannelRoomsWithPartnerAndLastMessage(userId, pageable);
        if (result.isEmpty()) {
            return null;
        }

        List<ChannelSummaryDto> list = result.getContent().stream()
                .map(p -> ChannelSummaryDto.fromProjectionWithDecrypt(p, aesUtil))
                .toList();

        return new ChannelListResponseDto(list, result.getNumber(), result.getSize(), result.isLast());
    }

    @Transactional
    public ChannelRoomResponseDto getChannelRoomMessages(Long roomId, Long userId, int page, int size) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(ChannelNotFoundException::new);

        if (!room.isParticipant(userId)) {
            throw new ForbiddenChannelException();
        }

        Long partnerId = room.getPartnerUser(userId).getId();
        // Todo: AI 쪽 DB에만 사용자 남아있는 경우 410 발생하며 모든 사용자 서비스 사용 불가능한 부분 리팩토링 필요
        User partner = userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new UserException("USER_DEACTIVATED", "상대방이 탈퇴한 사용자입니다."));

        Optional<RoomWithLastSenderProjection> result = signalMessageRepository.findRoomsWithLastSender(roomId);

        // 마지막 메세지를 보낸 사람이
        if(result.isPresent()){
            RoomWithLastSenderProjection lastSender = result.get();
            if (!Objects.equals(lastSender.getLastSenderId(), userId)) { // 내가 아닐 경우
                signalMessageRepository.markAllMessagesAsReadByRoomId(roomId); // isRead = true 처리
            }
        }

        asyncChannelService.notifyMatchingConvertedInChannelRoom(room, userId); // 비동기 실행

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendAt"));
        Page<SignalMessage> messagePage = signalMessageRepository.findBySignalRoom_Id(roomId, pageable);

        List<ChannelRoomResponseDto.MessageDto> messages = messagePage.getContent().stream()
                .map(msg -> ChannelRoomResponseDto.MessageDto.fromProjectionWithDecrypt(msg, aesUtil))
                .toList();

        return ChannelRoomResponseDto.of(roomId, partner, room.getRelationType(), messages, messagePage);
    }

    @Transactional
    public void sendChannelMessage(Long roomId, Long userId, SendSignalRequestDTO response) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(ChannelNotFoundException::new);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND, "사용자가 존재하지 않습니다."));

        if (!room.isParticipant(userId)) {
            throw new ForbiddenChannelException();
        }

        Long partnerId = room.getPartnerUser(userId).getId();
        userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_DEACTIVATED, "상대방이 탈퇴한 사용자입니다."));

        String encryptMessage = aesUtil.encrypt(response.getMessage());

        // 메시지 저장
        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(user)
                .message(encryptMessage)
                .build();

        signalMessageRepository.save(signalMessage);

        asyncChannelService.notifyMatchingConverted(room);
    }

    @Transactional
    public String channelMatchingStatusUpdate(Long userId, SignalMatchingRequestDTO response, MatchingStatus matchingStatus) {
        SignalRoom room = signalRoomRepository.findById(response.getChannelRoomId())
                .orElseThrow(ChannelNotFoundException::new);

        int updatedSender = signalRoomRepository.updateSenderMatchingStatus(room.getId(), userId, matchingStatus);
        int updatedReceiver = signalRoomRepository.updateReceiverMatchingStatus(room.getId(), userId, matchingStatus);

        if (updatedSender == 0 && updatedReceiver == 0) {
            throw new ForbiddenChannelException();
        }

        // 매칭 수락/거절 후 현재 관계
        if(matchingStatus == MatchingStatus.MATCHED) {
            return signalRoomRepository.findMatchResultByUser(userId, room.getId());
        } else {
            return ResponseCode.MATCH_REJECTION_SUCCESS;
        }
    }
}
