package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TuningRepository extends JpaRepository<Tuning, Long> {
    Optional<Tuning> findByUserAndCategory(User user, Category category);
}
