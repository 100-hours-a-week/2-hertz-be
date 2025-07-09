package com.hertz.hertz_be.domain.tuningreport.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TuningReportRepository extends JpaRepository<TuningReport, Long> {

    @Query("SELECT r FROM TuningReport r WHERE r.deletedAt IS NULL AND r.isVisible = true ORDER BY r.createdAt DESC")
    Page<TuningReport> findAllNotDeletedOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
    SELECT r FROM TuningReport r
    WHERE r.deletedAt IS NULL
    AND r.isVisible = true
    ORDER BY (
        r.reactionCelebrate +
        r.reactionThumbsUp +
        r.reactionLaugh +
        r.reactionEyes +
        r.reactionHeart
    ) DESC
""")
    Page<TuningReport> findAllNotDeletedOrderByTotalReactionDesc(Pageable pageable);

    @Query("SELECT r FROM TuningReport r WHERE r.signalRoom = :signalRoom AND r.deletedAt IS NULL")
    Optional<TuningReport> findNotDeletedBySignalRoom(@Param("signalRoom") SignalRoom signalRoom);

    List<TuningReport> findAllBySignalRoomIn(List<SignalRoom> rooms);

    @Modifying
    @Query("UPDATE TuningReport t SET t.deletedAt = CURRENT_TIMESTAMP WHERE t.id = :id")
    void softDeleteById(@org.springframework.data.repository.query.Param("id") Long id);

    // 교착 상태 예방용 비관적 락 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM TuningReport r WHERE r.id = :id")
    Optional<TuningReport> findWithLockById(@org.springframework.data.repository.query.Param("id") Long id);

}
