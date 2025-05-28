package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.TuningResult;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TuningResultRepository extends JpaRepository<TuningResult, Long> {
    List<TuningResult> findByTuning(Tuning tuning);

    Optional<TuningResult> findByTuningAndLineup(Tuning tuning, int lineup);

    boolean existsByTuningAndMatchedUser(Tuning tuning, User matchedUser);

    Optional<TuningResult> findFirstByTuningOrderByLineupAsc(Tuning tuning);

    boolean existsByTuning(Tuning tuning);

    void deleteAllByMatchedUser(User matchedUser);
}
