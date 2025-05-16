package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SignalRoomRepository extends JpaRepository<SignalRoom, Long> {
    boolean existsBySenderUserAndReceiverUser(User sender, User receiver);
    Optional<SignalRoom> findByUserPairSignal(String userPairSignal);

    // Todo: 웹소켓 도입 전 임시 사용 (v1), 추후 삭제 필요
    @Query(value = """
        SELECT 
            sr.id AS channelRoomId,
            u.profile_image_url AS partnerProfileImage,
            u.nickname AS partnerNickname,
            sm.message AS lastMessage,
            sm.send_at AS lastMessageTime,
            CASE
                WHEN sm.sender_user_id = :userId THEN 'true'
                WHEN sm.sender_user_id != :userId AND sm.is_read = true THEN 'true'
                ELSE 'false'
            END AS isRead,
            CASE
                WHEN sr.receiver_matching_status = 'MATCHED' AND sr.sender_matching_status = 'MATCHED'
                THEN 'MATCHING'
                ELSE 'SIGNAL'
            END AS relationType
        FROM signal_room sr
        JOIN user u ON 
            (CASE 
                WHEN sr.sender_user_id = :userId THEN sr.receiver_user_id 
                ELSE sr.sender_user_id 
             END) = u.id
        LEFT JOIN signal_message sm ON sm.id = (
            SELECT sm2.id
            FROM signal_message sm2
            WHERE sm2.signal_room_id = sr.id
            ORDER BY sm2.send_at DESC
            LIMIT 1
        )
        WHERE :userId IN (sr.sender_user_id, sr.receiver_user_id)
        ORDER BY sm.send_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM signal_room sr
        WHERE :userId IN (sr.sender_user_id, sr.receiver_user_id)
        """,
            nativeQuery = true)
    Page<ChannelRoomProjection> findChannelRoomsWithPartnerAndLastMessage(
            @Param("userId") Long userId,
            Pageable pageable
    );

}
