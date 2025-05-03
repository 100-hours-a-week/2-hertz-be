package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.dto.request.SendSignalRequestDTO;
import com.hertz.hertz_be.domain.channel.dto.response.SendSignalResponseDTO;
import com.hertz.hertz_be.domain.channel.exception.AlreadyInConversationException;
import com.hertz.hertz_be.domain.channel.exception.UserWithdrawnException;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final UserRepository userRepository;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;

    @Transactional
    public SendSignalResponseDTO sendSignal(Long senderUserId, SendSignalRequestDTO dto) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow();

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
                .signalRoomId(signalRoom)
                .senderUserId(sender)
                .message(dto.getMessage())
                .isRead(false)
                .build();
        signalMessageRepository.save(signalMessage);

        return new SendSignalResponseDTO(signalRoom.getId());
    }
}
