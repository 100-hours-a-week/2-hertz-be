package com.hertz.hertz_be.domain.user.repository;

import com.hertz.hertz_be.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain AND u.deletedAt IS NULL")
    List<User> findAllByEmailDomain(@Param("domain") String domain);

    @Query("""
    SELECT
        CASE
            WHEN sr.senderMatchingStatus = com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus.MATCHED
             AND sr.receiverMatchingStatus = com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus.MATCHED
            THEN 'MATCHING'
            ELSE 'SIGNAL'
        END
    FROM SignalRoom sr
    WHERE (sr.senderUser.id = :currentUserId AND sr.receiverUser.id = :targetUserId)
       OR (sr.senderUser.id = :targetUserId AND sr.receiverUser.id = :currentUserId)
    """)
    String findRelationTypeBetweenUsers(@Param("currentUserId") Long currentUserId,
                                          @Param("targetUserId") Long targetUserId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.sentSignalRooms WHERE u.id = :userId")
    Optional<User> findByIdWithSentSignalRooms(@Param("userId") Long userId);


    @Query("SELECT DISTINCT SUBSTRING_INDEX(u.email, '@', -1) FROM User u WHERE u.deletedAt IS NULL and u.id = :userId")
    String findDistinctEmailDomains(@Param("userId") Long userId);

}
