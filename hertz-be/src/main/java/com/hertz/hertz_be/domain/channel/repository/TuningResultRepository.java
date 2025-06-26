package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.TuningResult;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TuningResultRepository extends JpaRepository<TuningResult, Long> {
    void deleteAllByTuning(Tuning tuning);

    Optional<TuningResult> findFirstByTuningOrderByLineupAsc(Tuning tuning);

    boolean existsByTuning(Tuning tuning);

    void deleteAllByMatchedUser(User matchedUser);
}
