package com.hertz.hertz_be.domain.channel.fixture;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.user.entity.User;

import java.time.LocalDateTime;

public class SignalMessageFixture {

    public static SignalMessage create(User sender, String encryptedMessage) {
        return SignalMessage.builder()
                .message(encryptedMessage)
                .sendAt(LocalDateTime.now())
                .senderUser(sender)
                .isRead(true)
                .build();
    }

    public static SignalMessage createWithId(User sender, String encryptedMessage, Long id) {
        return SignalMessage.builder()
                .id(id)
                .message(encryptedMessage)
                .sendAt(LocalDateTime.now())
                .senderUser(sender)
                .isRead(true)
                .build();
    }
}
