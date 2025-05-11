package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.ChannelRoom;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRoomRepository extends JpaRepository<ChannelRoom, Long> {

// Todo: 웹소켓 도입 전 임시 사용 (v1), 추후 삭제 필요
    @Query(value = """
        SELECT 
            sr.id AS channelRoomId,
            u.profile_image_url AS partnerProfileImage,
            u.nickname AS partnerNickname,
            sm.message AS lastMessage,
            sm.send_at AS lastMessageTime,
            sm.is_read AS isRead,
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


// Todo: 추후 웹소켓 도입 시 쿼리 변경 필요 (v2)

//    @Query(value = """
//    SELECT
//        cr.id AS channelRoomId,
//        u.profile_image_url AS partnerProfileImage,
//        u.nickname AS partnerNickname,
//        cm.message AS lastMessage,
//        cm.send_at AS lastMessageTime,
//        CASE
//            WHEN cm.id <= COALESCE(cmlr.last_message_read_id, 0) THEN true
//            ELSE false
//        END AS isRead,
//        cr.category AS relationType
//    FROM channel_room cr
//    JOIN channel_join cj1 ON cj1.channel_room_id = cr.id
//    JOIN channel_join cj2 ON cj2.channel_room_id = cr.id AND cj2.user_id != :userId
//    JOIN user u ON u.id = cj2.user_id
//    LEFT JOIN channel_message cm ON cm.id = (
//        SELECT cm2.id FROM channel_message cm2
//        WHERE cm2.channel_room_id = cr.id
//        ORDER BY cm2.send_at DESC LIMIT 1
//    )
//    LEFT JOIN channel_message_last_read cmlr
//        ON cmlr.channel_id = cr.id AND cmlr.user_id = :userId
//    WHERE cr.current_user_count = 2
//      AND cj1.user_id = :userId
//    ORDER BY cm.send_at DESC
//    """,
//            countQuery = """
//    SELECT COUNT(*)
//    FROM channel_room cr
//    JOIN channel_join cj1 ON cj1.channel_room_id = cr.id
//    WHERE cr.current_user_count = 2
//      AND cj1.user_id = :userId
//    """,
//            nativeQuery = true)
//    Page<ChannelRoomProjection> findChannelRoomsWithPartnerAndLastMessage(
//            @Param("userId") Long userId,
//            Pageable pageable);


}
