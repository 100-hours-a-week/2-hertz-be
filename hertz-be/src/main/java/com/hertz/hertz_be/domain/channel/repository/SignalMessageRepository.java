package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.dto.object.UserMessageCountDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SignalMessageRepository extends JpaRepository<SignalMessage, Long> {
    boolean existsBySignalRoomInAndSenderUserNotAndIsReadFalse(List<SignalRoom> signalRooms, User senderUser);
    Page<SignalMessage> findBySignalRoom_Id(Long roomId, Pageable pageable);

    List<SignalMessage> findBySignalRoomIdAndSenderUserIdAndSendAtAfterOrderBySendAtAsc(
            Long signalRoomId,
            Long senderUserId,
            LocalDateTime sendAt
    );

    @Query("""
    SELECT new com.hertz.hertz_be.domain.channel.dto.object.UserMessageCountDto(
        m.senderUser.id, COUNT(m)
    )
    FROM SignalMessage m
    WHERE m.signalRoom.id = :roomId AND m.sendAt >= :filterDate
    GROUP BY m.senderUser.id
    """)
    List<UserMessageCountDto> countMessagesBySenderInRoomAfter(
            @Param("roomId") Long roomId,
            @Param("filterDate") LocalDateTime filterDate
    );

    @Query(value = """
    SELECT
        sm.sender_user_id AS lastSenderId
    FROM signal_room sr
    LEFT JOIN signal_message sm ON sm.id = (
        SELECT sm2.id
        FROM signal_message sm2
        WHERE sm2.signal_room_id = sr.id
        ORDER BY sm2.send_at DESC
        LIMIT 1
    )
    LEFT JOIN user u ON sm.sender_user_id = u.id
    WHERE sr.id = :roomId
    """, nativeQuery = true)
    Optional<RoomWithLastSenderProjection> findRoomsWithLastSender(@Param("roomId") Long roomId);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE SignalMessage sm SET sm.isRead = true WHERE sm.signalRoom.id = :roomId")
    int markAllMessagesAsReadByRoomId(@Param("roomId") Long roomId);

    void deleteAllBySignalRoom(SignalRoom signalRoom);

    int countBySignalRoom(SignalRoom signalRoom);

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE SignalMessage sm
    SET sm.isRead = true
    WHERE sm.signalRoom.id = :roomId
      AND sm.senderUser.id != :userId
      AND sm.isRead = false
""")
    void markUnreadMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
