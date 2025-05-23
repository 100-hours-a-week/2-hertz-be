package com.hertz.hertz_be.domain.channel.dto.response;

import com.hertz.hertz_be.global.util.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DecryptedChannelSummaryConverter {

    private final AESUtil aesUtil;

    public ChannelSummaryDto from(ChannelSummaryDto encryptedDto) {
        String decryptedMessage;
        try {
            decryptedMessage = aesUtil.decrypt(encryptedDto.getLastMessage());
        } catch (RuntimeException e) {
            decryptedMessage = "메세지를 표시할 수 없습니다.";
        }

        return new ChannelSummaryDto(
                encryptedDto.getChannelRoomId(),
                encryptedDto.getPartnerProfileImage(),
                encryptedDto.getPartnerNickname(),
                decryptedMessage,
                encryptedDto.getLastMessageTime(),
                encryptedDto.isRead(),
                encryptedDto.getRelationType()
        );
    }
}
