package com.hertz.hertz_be.domain.channel.fixture;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.user.entity.User;

public class SignalRoomFixture {

    public static SignalRoom createMatchedRoom(User sender, User receiver) {
        return SignalRoom.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .senderMatchingStatus(MatchingStatus.MATCHED)
                .receiverMatchingStatus(MatchingStatus.MATCHED)
                .build();
    }

    public static SignalRoom createUnmatchedRoom(User sender, User receiver) {
        return SignalRoom.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .senderMatchingStatus(MatchingStatus.UNMATCHED)
                .receiverMatchingStatus(MatchingStatus.MATCHED)
                .build();
    }

    public static SignalRoom createWithId(User sender, User receiver, Long roomId) {
        return SignalRoom.builder()
                .id(roomId)
                .senderUser(sender)
                .receiverUser(receiver)
                .senderMatchingStatus(MatchingStatus.SIGNAL)
                .receiverMatchingStatus(MatchingStatus.SIGNAL)
                .category(Category.FRIEND)
                .userPairSignal("sig-" + sender.getId() + "-" + receiver.getId())
                .build();
    }
}
