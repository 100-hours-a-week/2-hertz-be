package com.hertz.hertz_be.domain.tuningreport.repository;

import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TuningReportRepository extends JpaRepository<TuningReport, Long> {
    Page<TuningReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Query("""
    SELECT r FROM TuningReport r
    ORDER BY (
        r.reactionCelebrate +
        r.reactionThumbsUp +
        r.reactionLaugh +
        r.reactionEyes +
        r.reactionHeart
    ) DESC
""")
    Page<TuningReport> findAllOrderByTotalReactionDesc(Pageable pageable);
}
