package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.*;
import com.hertz.hertz_be.domain.channel.entity.*;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.exception.*;
import com.hertz.hertz_be.domain.channel.repository.*;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.exception.AiServerErrorException;
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
    private final ChannelRoomRepository channelRoomRepository;
    private final WebClient webClient;

    @Autowired
    public ChannelService(UserRepository userRepository,
                          TuningRepository tuningRepository,
                          TuningResultRepository tuningResultRepository,
                          UserInterestsRepository userInterestsRepository,
                          SignalRoomRepository signalRoomRepository,
                          SignalMessageRepository signalMessageRepository,
                          ChannelRoomRepository channelRoomRepository,
                          @Value("${ai.server.ip}") String aiServerIp) {
        this.userRepository = userRepository;
        this.tuningRepository = tuningRepository;
        this.tuningResultRepository = tuningResultRepository;
        this.userInterestsRepository = userInterestsRepository;
        this.signalMessageRepository = signalMessageRepository;
        this.signalRoomRepository = signalRoomRepository;
        this.channelRoomRepository = channelRoomRepository;
        this.webClient = WebClient.builder().baseUrl(aiServerIp).build();
    }

    @Transactional
    public SendSignalResponseDTO sendSignal(Long senderUserId, SendSignalRequestDTO dto) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(InternalServerErrorException::new);

        User receiver = userRepository.findById(dto.getReceiverUserId())
                .orElseThrow(UserWithdrawnException::new);

        // user_pair_signal 컬럼 누락으로 인한 수정 진행
//        boolean alreadyExists = signalRoomRepository.existsBySenderUserAndReceiverUser(sender, receiver);
//        if (alreadyExists) {
//            throw new AlreadyInConversationException();
//        }

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

        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(signalRoom)
                .senderUser(sender)
                .message(dto.getMessage())
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

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(InternalServerErrorException::new);
    }


    @Transactional
    public TuningResponseDTO getTunedUser(Long userId) {
        User requester = getUserById(userId);

        boolean isUserChooseInterests = userInterestsRepository.existsByUser(requester);
        if (!isUserChooseInterests){
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
            case "TUNING_SUCCESS_BUT_NO_MATCH" -> {
                return false;
            }
            case "TUNING_BAD_REQUEST", "TUNING_NOT_FOUND_USER", "TUNING_INTERNAL_SERVER_ERROR" ->
                    throw new AiServerErrorException();
            case "TUNING_SUCCESS" -> {
                Object dataObj = responseMap.get("data");
                if (!(dataObj instanceof Map data)) {
                    throw new AiServerErrorException();
                }

                List<Integer> userIdList = (List<Integer>) data.get("userIdList");
                if (userIdList == null || userIdList.isEmpty()) {
                    throw new AiServerErrorException();
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
            throw new AiServerErrorException();
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
                    .orElseThrow(InternalServerErrorException::new);

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
                .orElseThrow(InternalServerErrorException::new);

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
        Page<ChannelRoomProjection> result = channelRoomRepository.findChannelRoomsWithPartnerAndLastMessage(userId, pageable);
        if (result.isEmpty()) {
            return null;
        }

        List<ChannelSummaryDto> list = result.getContent().stream()
                .map(ChannelSummaryDto::fromProjection)
                .toList();

        return new ChannelListResponseDto(list, result.getNumber(), result.getSize(), result.isLast());
    }

    @Transactional(readOnly = true)
    public ChannelRoomResponseDto getChannelRoomMessages(Long roomId, Long userId, int page, int size) {
        SignalRoom room = signalRoomRepository.findById(roomId)
                .orElseThrow(ChannelNotFoundException::new);

        if (!room.isParticipant(userId)) {
            throw new UnauthorizedAccessException();
        }

        Long partnerId = room.getPartnerUser(userId).getId();
        User partner = userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new UserException("USER_DEACTIVATED", "상대방이 탈퇴한 사용자입니다."));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sendAt"));
        Page<SignalMessage> messagePage = signalMessageRepository.findBySignalRoom_Id(roomId, pageable);

        List<ChannelRoomResponseDto.MessageDto> messages = messagePage.getContent().stream()
                .map(ChannelRoomResponseDto.MessageDto::from)
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
            throw new UnauthorizedAccessException();
        }

        Long partnerId = room.getPartnerUser(userId).getId();
        userRepository.findByIdAndDeletedAtIsNull(partnerId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_DEACTIVATED, "상대방이 탈퇴한 사용자입니다."));

        // 메시지 저장
        SignalMessage signalMessage = SignalMessage.builder()
                .signalRoom(room)
                .senderUser(user)
                .message(response.getMessage())
                .build();

        signalMessageRepository.save(signalMessage);
    }
}
