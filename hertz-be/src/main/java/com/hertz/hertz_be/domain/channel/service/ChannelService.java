package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.*;
import com.hertz.hertz_be.domain.channel.entity.ChannelMessage;
import com.hertz.hertz_be.domain.channel.entity.ChannelRoom;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.exception.AlreadyInConversationException;
import com.hertz.hertz_be.domain.channel.exception.ChannelNotFoundException;
import com.hertz.hertz_be.domain.channel.exception.UnauthorizedAccessException;
import com.hertz.hertz_be.domain.channel.exception.UserWithdrawnException;
import com.hertz.hertz_be.domain.channel.repository.ChannelRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.exception.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final UserRepository userRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final ChannelRoomRepository channelRoomRepository;

    @Transactional
    public SendSignalResponseDTO sendSignal(Long senderUserId, SendSignalRequestDTO dto) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(InternalServerErrorException::new);

        User receiver = userRepository.findById(dto.getReceiverUserId())
                .orElseThrow(UserWithdrawnException::new);

        boolean alreadyExists = signalRoomRepository.existsBySenderUserAndReceiverUser(sender, receiver);
        if (alreadyExists) {
            throw new AlreadyInConversationException();
        }

        SignalRoom signalRoom = SignalRoom.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .category(Category.FRIEND)
                .senderMatchingStatus(MatchingStatus.SIGNAL)
                .receiverMatchingStatus(MatchingStatus.SIGNAL)
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

    public TuningResponseDTO getTunedUser(Long userId) {
        return new TuningResponseDTO(
                2L,
                "../image/profile.jpg",
                "행복한 개구리",
                "남성",
                "안녕하세요, 프론트엔드 개발자입니다.",
                Map.of(
                        "MBTI", "ESTP",
                        "religion", "NON_RELIGIOUS",
                        "smoking", "NO_SMOKING",
                        "drinking", "SOMETIMES"
                ),
                Map.of(
                        "personality", List.of(),
                        "preferredPeople", List.of("DOESNT_SWEAR"),
                        "currentInterests", List.of(),
                        "favoriteFoods", List.of("STREET_FOOD"),
                        "likedSports", List.of("YOGA"),
                        "pets", List.of("RABBIT"),
                        "selfDevelopment", List.of("DIET"),
                        "hobbies", List.of("GAMING")
                )
        );
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

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sendAt"));
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
