package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.projection.SignalRoomRepositoryCustom;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SignalRoomRepository extends JpaRepository<SignalRoom, Long>, SignalRoomRepositoryCustom {
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
    SELECT sr.*
    FROM signal_room sr
    LEFT JOIN (
        SELECT sm.signal_room_id, MAX(sm.send_at) AS last_message_time
        FROM signal_message sm
        GROUP BY sm.signal_room_id
    ) sm_last ON sr.id = sm_last.signal_room_id
    WHERE sr.sender_user_id = :userId OR sr.receiver_user_id = :userId
    ORDER BY sm_last.last_message_time DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM signal_room sr
    WHERE sr.sender_user_id = :userId OR sr.receiver_user_id = :userId
    """,
            nativeQuery = true)
    Page<SignalRoom> findAllOrderByLastMessageTimeDesc(
            @Param("userId") Long userId,
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

    @Query(value = """
    SELECT CEIL(COUNT(*) * 1.0 / :pageSize) - 1
    FROM signal_message
    WHERE signal_room_id = :signalRoomId
    """, nativeQuery = true)
    int findLastPageNumberBySignalRoomId(@Param("signalRoomId") Long signalRoomId, @Param("pageSize") int pageSize);

}
