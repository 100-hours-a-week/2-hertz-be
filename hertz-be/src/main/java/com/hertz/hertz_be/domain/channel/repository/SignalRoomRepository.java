package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SignalRoomRepository extends JpaRepository<SignalRoom, Long> {
    boolean existsBySenderUserAndReceiverUser(User sender, User receiver);

    Optional<SignalRoom> findByUserPairSignal(String userPairSignal);

    @Query("""
    SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END 
    FROM SignalRoom sr 
    WHERE 
        ((sr.senderUser = :user1 AND sr.receiverUser = :user2) 
        OR (sr.senderUser = :user2 AND sr.receiverUser = :user1))
        AND sr.category = :category
""")
    boolean existsByUserPairAndCategory(@Param("user1") User user1,
                                        @Param("user2") User user2,
                                        @Param("category") Category category);

    @Query(value = """
    SELECT 
        sr.id AS channelRoomId,
        u.profile_image_url AS partnerProfileImage,
        u.nickname AS partnerNickname,
        sm.message AS lastMessage,
        sm.send_at AS lastMessageTime,
        sr.sender_user_id AS senderUserId,
        sr.receiver_user_id AS receiverUserId,
        sr.sender_exited_at AS senderExitedAt,
        sr.receiver_exited_at AS receiverExitedAt,
        sr.category AS category,
        CASE
            WHEN sm.sender_user_id = :userId THEN 'true'
            WHEN sm.sender_user_id != :userId AND sm.is_read = true THEN 'true'
            ELSE 'false'
        END AS isRead,
        CASE
            WHEN sr.sender_matching_status = 'MATCHED' AND sr.receiver_matching_status = 'MATCHED'
                THEN 'MATCHING'
            WHEN sr.sender_matching_status = 'UNMATCHED' OR sr.receiver_matching_status = 'UNMATCHED'
                THEN 'UNMATCHED'
            ELSE 'SIGNAL'
        END AS relationType,
        CEIL(
            (SELECT COUNT(*) FROM signal_message sm3 WHERE sm3.signal_room_id = sr.id) / :pageSize * 1.0) - 1 AS lastPageNumber
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
            @Param("pageSize") int pageSize,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE SignalRoom sr
        SET sr.senderMatchingStatus = :status
        WHERE sr.id = :roomId AND (sr.senderUser.id = :userId)
    """)
    int updateSenderMatchingStatus(@Param("roomId") Long roomId,
                                   @Param("userId") Long userId,
                                   @Param("status") MatchingStatus status);

    @Modifying
    @Transactional
    @Query("""
        UPDATE SignalRoom sr
        SET sr.receiverMatchingStatus = :status
        WHERE sr.id = :roomId AND (sr.receiverUser.id = :userId)
    """)
    int updateReceiverMatchingStatus(@Param("roomId") Long roomId,
                                     @Param("userId") Long userId,
                                     @Param("status") MatchingStatus status);


    @Query("""
    SELECT 
        CASE
            WHEN (u.deletedAt IS NOT NULL) THEN 'USER_DEACTIVATED'
            WHEN (:userId = sr.senderUser.id AND sr.senderMatchingStatus = 'UNMATCHED')
              OR (:userId = sr.receiverUser.id AND sr.receiverMatchingStatus = 'UNMATCHED')
              THEN 'MATCH_FAILED'
            WHEN (sr.senderMatchingStatus = 'MATCHED' AND sr.receiverMatchingStatus = 'MATCHED')
              THEN 'MATCH_SUCCESS'
            WHEN (:userId = sr.senderUser.id AND sr.senderMatchingStatus = 'MATCHED' AND sr.receiverMatchingStatus = 'SIGNAL')
              OR (:userId = sr.receiverUser.id AND sr.receiverMatchingStatus = 'MATCHED' AND sr.senderMatchingStatus = 'SIGNAL')
              THEN 'MATCH_PENDING'
            ELSE 'MATCH_FAILED'
        END
    FROM SignalRoom sr
    JOIN User u ON 
        CASE 
            WHEN sr.senderUser.id = :userId THEN sr.receiverUser.id
            ELSE sr.senderUser.id
        END = u.id
    WHERE sr.id = :roomId
""")
    String findMatchResultByUser(@Param("userId") Long userId, @Param("roomId") Long roomId);

    List<SignalRoom> findAllBySenderUserIdOrReceiverUserId(Long senderId, Long receiverId);

    @Query("""
        SELECT sr.id FROM SignalRoom sr
        WHERE sr.senderUser.id = :userId OR sr.receiverUser.id = :userId
    """)
    List<Long> findRoomIdsByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT DISTINCT sr FROM SignalRoom sr
        JOIN FETCH sr.senderUser sender
        JOIN FETCH sr.receiverUser receiver
        LEFT JOIN FETCH sr.tuningReport tr
        WHERE sr.category = :category
          AND sr.senderMatchingStatus = com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus.MATCHED
          AND sr.receiverMatchingStatus = com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus.MATCHED
          AND (
            tr IS NULL OR tr.isVisible = false OR tr.deletedAt IS NOT NULL
          )
          AND sr.createdAt <= :endDateTime
""")
    List<SignalRoom> findEligibleRooms(@Param("category") Category category,
                                       @Param("end") LocalDateTime endDateTime);
}
