package com.hertz.hertz_be.domain.tuningreport.repository;

import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TuningReportUserReactionRepository extends JpaRepository<TuningReportUserReaction, Long> {

    List<TuningReportUserReaction> findAllByUserIdAndReportIdIn(Long userId, List<Long> reportIds);
    boolean existsByReportIdAndUserIdAndReactionType(Long reportId, Long userId, ReactionType reactionType);
    void deleteByReportIdAndUserIdAndReactionType(Long reportId, Long userId, ReactionType reactionType);

    List<TuningReportUserReaction> findAllByReportId(Long id);
}
