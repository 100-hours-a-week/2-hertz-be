package com.hertz.hertz_be.domain.channel.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.util.AESUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelRoomResponseDto {

    private Long channelRoomId;
    private Long partnerId;
    private String partnerProfileImage;
    private String partnerNickname;
    private String relationType;
    private MessagePage messages;

    public static ChannelRoomResponseDto of(Long roomId, User partner, String relationType,
                                         List<MessageDto> messages, Page<SignalMessage> page) {
        return ChannelRoomResponseDto.builder()
                .channelRoomId(roomId)
                .partnerId(partner.getId())
                .partnerProfileImage(partner.getProfileImageUrl())
                .partnerNickname(partner.getNickname())
                .relationType(relationType)
                .messages(new MessagePage(messages, page))
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class MessageDto {
        private Long messageId;
        private Long messageSenderId;
        private String messageContents;
        private String messageSendAt;

        public static MessageDto fromProjectionWithDecrypt(SignalMessage msg, AESUtil aesUtil) {
            String decryptedMessage;
            try {
                decryptedMessage = aesUtil.decrypt(msg.getMessage());
            } catch (Exception e) {
                decryptedMessage = "메세지를 표시할 수 없습니다.";
            }

            return new MessageDto(
                    msg.getId(),
                    msg.getSenderUser().getId(),
                    decryptedMessage,
                    msg.getSendAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class MessagePage {
        private List<MessageDto> list;
        private PageableInfo pageable;

        public MessagePage(List<MessageDto> list, Page<?> page) {
            this.list = list;
            this.pageable = new PageableInfo(page);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PageableInfo {
        private int pageNumber;
        private int pageSize;
        @JsonProperty("isLast")
        private boolean isLast;

        public PageableInfo(Page<?> page) {
            this.pageNumber = page.getNumber();
            this.pageSize = page.getSize();
            this.isLast = page.isLast();
        }
    }
}
